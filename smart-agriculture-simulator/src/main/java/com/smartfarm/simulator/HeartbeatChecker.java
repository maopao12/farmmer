package com.smartfarm.simulator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.AlertLog;
import com.smartfarm.business.entity.Device;
import com.smartfarm.business.mapper.AlertLogMapper;
import com.smartfarm.business.mapper.DeviceMapper;
import com.smartfarm.framework.websocket.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备心跳检测器
 *
 * <pre>
 * 每分钟执行一次: 扫描所有标记为ONLINE的设备
 * 若 last_heartbeat 距当前时间超过配置阈值 → 自动标记为OFFLINE
 *   → WebSocket推送设备状态变更 (前端UI置灰设备卡片)
 *   → 写入告警日志 (告警级别: WARNING)
 *
 * 超时阈值可通过 smartfarm.heartbeat.timeout 配置（默认3分钟）
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatChecker {

    private final DeviceMapper deviceMapper;
    private final AlertLogMapper alertLogMapper;
    private final WsPushService wsPushService;

    /** 心跳超时阈值(分钟) — 从配置文件读取 */
    @Value("${smartfarm.heartbeat.timeout:3}")
    private int heartbeatTimeoutMinutes;

    /**
     * 每分钟执行心跳检测
     */
    @Scheduled(fixedRate = 60000)
    public void checkHeartbeat() {
        List<Device> onlineDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getStatus, "ONLINE")
        );

        if (onlineDevices.isEmpty()) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(heartbeatTimeoutMinutes);

        for (Device device : onlineDevices) {
            // 从未有心跳或心跳超时
            if (device.getLastHeartbeat() == null ||
                    device.getLastHeartbeat().isBefore(threshold)) {

                log.warn("[心跳超时] 设备{}({}) 超过{}分钟无心跳，自动标记为OFFLINE",
                        device.getDeviceCode(), device.getDeviceName(),
                        heartbeatTimeoutMinutes);

                // 更新设备状态
                device.setStatus("OFFLINE");
                deviceMapper.updateById(device);

                // WebSocket推送设备离线通知 → 前端UI置灰设备卡片
                wsPushService.sendDeviceStatusChange(device.getId(), "OFFLINE");

                // 写入告警日志
                AlertLog alertLog = AlertLog.builder()
                        .deviceId(device.getId())
                        .plotId(device.getPlotId())
                        .alertMsg(String.format("设备 [%s] 超过%d分钟无心跳，已自动标记为离线",
                                device.getDeviceName(), heartbeatTimeoutMinutes))
                        .alertLevel("WARNING")
                        .triggerTime(LocalDateTime.now())
                        .isRead(0)
                        .build();
                alertLogMapper.insert(alertLog);

                // 如果设备绑定了地块，推送告警给对应农户
                if (device.getPlotId() != null) {
                    wsPushService.sendAlert(device.getPlotId(), alertLog.getId(),
                            alertLog.getDeviceId(), alertLog.getAlertMsg(),
                            alertLog.getAlertLevel(), alertLog.getCurrentValue(),
                            alertLog.getTriggerTime());
                }
            }
        }
    }
}
