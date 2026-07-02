package com.smartfarm.business.control;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.ControlLog;
import com.smartfarm.business.entity.Device;
import com.smartfarm.business.entity.Plot;
import com.smartfarm.business.mapper.ControlLogMapper;
import com.smartfarm.business.mapper.DeviceMapper;
import com.smartfarm.business.mapper.PlotMapper;
import com.smartfarm.common.exception.BizException;
import com.smartfarm.framework.security.UserContext;
import com.smartfarm.framework.websocket.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 设备控制服务 — 四级强校验 + 指令超时闭环 + 审计日志永不丢失
 *
 * <pre>
 * 【事务隔离设计 (v2.1 修复)】
 *   本类方法不标注 @Transactional。
 *   所有 control_log 的 INSERT/UPDATE 通过 ControlLogService 执行，
 *   后者使用 REQUIRES_NEW 确保审计记录独立提交。
 *
 *   即使 executeCommand 抛出 TimeoutException，
 *   SENT 状态的日志已由 ControlLogService.insertSentLog() 持久化，
 *   后续的 TIMEOUT 状态更新同样在独立事务中提交。
 *
 * 【四级校验链】
 *   ① 归属权校验：设备所属 plot.owner_id == currentUserId（ADMIN跳过）
 *   ② 设备类型校验：device_category MUST be CONTROLLER
 *   ③ 在线状态校验：status MUST be ONLINE
 *   ④ 日志强一致性：ControlLogService (REQUIRES_NEW)
 *
 * 【超时闭环（10秒）】
 *   SENT → 10s等待 → SUCCESS / TIMEOUT / FAILED
 *   所有终态通过 WebSocket 推送给前端
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ControlService {

    private final DeviceMapper deviceMapper;
    private final PlotMapper plotMapper;
    private final ControlLogMapper controlLogMapper;
    private final ControlLogService controlLogService;
    private final WsPushService wsPushService;

    /**
     * 设备响应等待池 — Key: controlLogId, Value: CompletableFuture
     * 生产环境应替换为 Redis / 分布式缓存
     */
    private final Map<Long, CompletableFuture<String>> responseFutures = new ConcurrentHashMap<>();

    /** 指令响应超时(秒) — 从配置文件读取 */
    @Value("${smartfarm.command.timeout:10}")
    private long timeoutSeconds;

    /**
     * 下发灌溉控制指令。
     *
     * <p><b>注意：本方法不标注 @Transactional。</b>
     * 审计日志通过 {@link ControlLogService} 的 REQUIRES_NEW 独立提交，
     * 确保即使本方法抛出异常，审计记录也不回滚。</p>
     *
     * @param deviceId 目标设备ID
     * @param command  指令: ON / OFF
     * @param duration 定时时长(分钟)，可选
     * @return 指令执行结果
     */
    public ControlLog executeCommand(Long deviceId, String command, Integer duration) {
        Long currentUserId = UserContext.getCurrentUserId();

        // ==================== 步骤①：归属权校验 ====================
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BizException(404, "设备不存在");
        }
        if (device.getPlotId() == null) {
            throw BizException.noPermission();
        }
        Plot plot = plotMapper.selectById(device.getPlotId());
        if (plot == null) {
            throw BizException.noPermission();
        }
        // RBAC: ADMIN 跳过归属校验，FARMER 校验 owner_id
        if (!UserContext.isAdmin() && !plot.getOwnerId().equals(currentUserId)) {
            log.warn("[越权拦截] 用户{} 尝试操作不属于自己的设备{}", currentUserId, deviceId);
            throw BizException.noPermission();
        }

        // ==================== 步骤②：设备类型校验 ====================
        if (!"CONTROLLER".equals(device.getDeviceCategory())) {
            log.warn("[类型拦截] 设备{} 为SENSOR类型，不支持控制", device.getDeviceCode());
            throw BizException.sensorNotControllable(device.getDeviceName());
        }

        // ==================== 步骤③：在线状态校验 ====================
        if (!"ONLINE".equals(device.getStatus())) {
            log.warn("[离线拦截] 设备{} 当前离线(status={})", device.getDeviceCode(), device.getStatus());

            // 离线也写审计日志 — REQUIRES_NEW，独立提交，永不回滚
            ControlLog offlineLog = buildControlLog(deviceId, plot.getId(), currentUserId,
                    command, duration);
            controlLogService.insertOfflineLog(offlineLog);

            throw BizException.deviceOffline(device.getDeviceName());
        }

        // ==================== 步骤④：写入审计日志(REQUIRES_NEW) ====================
        // 关键修复：日志立即在独立事务中提交，后续超时异常不会回滚此记录
        ControlLog controlLog = buildControlLog(deviceId, plot.getId(), currentUserId,
                command, duration);
        controlLog = controlLogService.insertSentLog(controlLog);

        log.info("[指令下发] 用户{} 对设备{}({}) 下发指令: {}, 日志ID: {}",
                currentUserId, device.getDeviceCode(), device.getDeviceName(),
                command, controlLog.getId());

        // ==================== 超时闭环：10秒等待设备响应 ====================
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseFutures.put(controlLog.getId(), responseFuture);

        try {
            // 通过MQTT下发指令（校内Mock模式走模拟响应）
            publishMqttCommand(device, command, duration);

            // 阻塞等待设备响应，最多10秒
            String deviceResponse = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);

            // 收到响应 → SUCCESS (REQUIRES_NEW)
            controlLogService.markSuccess(controlLog.getId(), deviceResponse, LocalDateTime.now());

            log.info("[指令成功] 设备{} 响应: {}", device.getDeviceCode(), deviceResponse);

            // 重新查询最新状态用于WebSocket推送
            ControlLog updatedLog = controlLogMapper.selectById(controlLog.getId());
            wsPushService.sendCommandResult(deviceId, updatedLog.getCommand(),
                    updatedLog.getCommandStatus(), updatedLog.getResultMsg(),
                    updatedLog.getResponseTime());

            return updatedLog;

        } catch (TimeoutException e) {
            // ========== 超时：审计日志独立更新(REQUIRES_NEW) — 不回滚 ==========
            controlLogService.markTimeout(controlLog.getId(),
                    "设备连接超时，10秒内未收到响应");

            log.error("[指令超时] 设备{} 10秒内无响应, 日志ID: {}",
                    device.getDeviceCode(), controlLog.getId());

            // WebSocket推送超时通知
            ControlLog timeoutLog = controlLogMapper.selectById(controlLog.getId());
            wsPushService.sendCommandResult(deviceId, timeoutLog.getCommand(),
                    timeoutLog.getCommandStatus(), timeoutLog.getResultMsg(),
                    timeoutLog.getResponseTime());

            throw BizException.commandTimeout(device.getDeviceName());

        } catch (Exception e) {
            // ========== 异常：审计日志独立更新(REQUIRES_NEW) — 不回滚 ==========
            controlLogService.markFailed(controlLog.getId(),
                    "指令执行异常: " + e.getMessage());

            log.error("[指令异常] 设备{} 执行异常", device.getDeviceCode(), e);

            ControlLog failedLog = controlLogMapper.selectById(controlLog.getId());
            wsPushService.sendCommandResult(deviceId, failedLog.getCommand(),
                    failedLog.getCommandStatus(), failedLog.getResultMsg(),
                    failedLog.getResponseTime());

            throw new BizException(500, "指令执行失败: " + e.getMessage());

        } finally {
            responseFutures.remove(controlLog.getId());
        }
    }

    /**
     * 接收设备MQTT响应 — 由MQTT消息监听器或Mock回调线程调用
     */
    public void onDeviceResponse(Long controlLogId, String response) {
        CompletableFuture<String> future = responseFutures.get(controlLogId);
        if (future != null && !future.isDone()) {
            future.complete(response);
            log.info("[设备响应] controlLogId={}, response={}", controlLogId, response);
        } else {
            log.warn("[设备响应] 未找到等待中的指令或已完成, controlLogId={}", controlLogId);
        }
    }

    // ==================== 私有方法 ====================

    private ControlLog buildControlLog(Long deviceId, Long plotId, Long operatorId,
                                        String command, Integer duration) {
        return ControlLog.builder()
                .deviceId(deviceId)
                .plotId(plotId)
                .operatorId(operatorId)
                .command(command)
                .commandParams(duration != null ? "{\"duration\":" + duration + "}" : null)
                .build();
    }

    private void publishMqttCommand(Device device, String command, Integer duration) {
        log.info("[MQTT模拟] 向设备{} Topic[{}] 发送指令: {}",
                device.getDeviceCode(), device.getMqttTopic(), command);

        // Mock模式: 模拟2秒后设备自动响应（生产环境删除此行）
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
            ControlLog latestLog = controlLogMapper.selectOne(
                    new LambdaQueryWrapper<ControlLog>()
                            .eq(ControlLog::getDeviceId, device.getId())
                            .eq(ControlLog::getCommandStatus, "SENT")
                            .orderByDesc(ControlLog::getSendTime)
                            .last("LIMIT 1")
            );
            if (latestLog != null) {
                String mockResponse = String.format(
                        "{\"status\":\"ok\",\"device\":\"%s\",\"cmd\":\"%s\",\"timestamp\":\"%s\"}",
                        device.getDeviceCode(), command, LocalDateTime.now());
                this.onDeviceResponse(latestLog.getId(), mockResponse);
            }
        });
    }
}
