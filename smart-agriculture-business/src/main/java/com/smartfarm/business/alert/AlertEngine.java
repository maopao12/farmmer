package com.smartfarm.business.alert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.*;
import com.smartfarm.business.mapper.AlertLogMapper;
import com.smartfarm.business.mapper.AlertRuleMapper;
import com.smartfarm.business.mapper.SensorDataMapper;
import com.smartfarm.framework.websocket.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警引擎 — 每次传感器数据更新后触发扫描
 * <p>
 * 告警规则基于 alert_rule 表动态加载，支持:
 *   - 下限阈值(min_threshold): 当前值 < 阈值 → 告警
 *   - 上限阈值(max_threshold): 当前值 > 阈值 → 告警
 *
 * @author SmartFarm Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEngine {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertLogMapper alertLogMapper;
    private final SensorDataMapper sensorDataMapper;
    private final WsPushService wsPushService;

    /**
     * 批量扫描 — 对活跃SENSOR列表逐一检查
     */
    public void scan(List<Device> activeSensors) {
        for (Device sensor : activeSensors) {
            try {
                checkDevice(sensor);
            } catch (Exception e) {
                log.error("[告警引擎] 设备{}扫描异常: {}", sensor.getDeviceCode(), e.getMessage());
            }
        }
    }

    /**
     * 检查单个设备是否需要触发告警
     */
    private void checkDevice(Device sensor) {
        // 查询该设备启用的告警规则
        List<AlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>()
                        .eq(AlertRule::getDeviceId, sensor.getId())
                        .eq(AlertRule::getEnabled, 1)
        );

        for (AlertRule rule : rules) {
            // 获取最新的传感器数据
            SensorData latestData = sensorDataMapper.selectOne(
                    new LambdaQueryWrapper<SensorData>()
                            .eq(SensorData::getDeviceId, sensor.getId())
                            .eq(SensorData::getDataType, rule.getMetricType())
                            .orderByDesc(SensorData::getCollectTime)
                            .last("LIMIT 1")
            );

            if (latestData == null) continue;

            BigDecimal currentValue = latestData.getDataValue();
            boolean alertTriggered = false;
            String alertMsg = null;

            // 下限检查: currentValue < minThreshold
            if (rule.getMinThreshold() != null &&
                    currentValue.compareTo(rule.getMinThreshold()) < 0) {
                alertTriggered = true;
                alertMsg = String.format("[%s] %s低于下限: 当前值=%s, 阈值=%s",
                        rule.getMetricType(), rule.getRuleName(),
                        currentValue, rule.getMinThreshold());
            }

            // 上限检查: currentValue > maxThreshold
            if (rule.getMaxThreshold() != null &&
                    currentValue.compareTo(rule.getMaxThreshold()) > 0) {
                alertTriggered = true;
                alertMsg = String.format("[%s] %s超过上限: 当前值=%s, 阈值=%s",
                        rule.getMetricType(), rule.getRuleName(),
                        currentValue, rule.getMaxThreshold());
            }

            if (alertTriggered && alertMsg != null) {
                // 写入告警日志
                AlertLog alertLog = AlertLog.builder()
                        .ruleId(rule.getId())
                        .deviceId(sensor.getId())
                        .plotId(sensor.getPlotId())
                        .alertMsg(alertMsg)
                        .alertLevel(rule.getAlertLevel())
                        .currentValue(currentValue)
                        .triggerTime(LocalDateTime.now())
                        .isRead(0)
                        .build();
                alertLogMapper.insert(alertLog);

                // 如果是严重告警，WebSocket实时推送
                if ("CRITICAL".equals(rule.getAlertLevel())) {
                    wsPushService.sendAlert(sensor.getPlotId(), alertLog.getId(),
                            alertLog.getDeviceId(), alertLog.getAlertMsg(),
                            alertLog.getAlertLevel(), alertLog.getCurrentValue(),
                            alertLog.getTriggerTime());
                }

                log.warn("[告警触发] 设备{} 触发规则: {}", sensor.getDeviceCode(), alertMsg);
            }
        }
    }
}
