package com.smartfarm.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WebSocket 推送事件DTO — 位于 common 模块，所有模块可共享
 *
 * @author SmartFarm Team
 */
public final class WsEvent {

    private WsEvent() {}

    /** 传感器实时数据推送 */
    public record SensorDataEvent(
            Long deviceId,
            String dataType,
            BigDecimal dataValue,
            String unit,
            LocalDateTime collectTime,
            String source
    ) {}

    /** 告警通知推送 */
    public record AlertEvent(
            Long alertId,
            Long deviceId,
            Long plotId,
            String alertMsg,
            String alertLevel,
            BigDecimal currentValue,
            LocalDateTime triggerTime
    ) {}

    /** 设备状态变更推送 (ONLINE→OFFLINE) */
    public record DeviceStatusEvent(
            Long deviceId,
            String status
    ) {}

    /** 控制指令执行结果推送 */
    public record CommandResultEvent(
            Long deviceId,
            String command,
            String status,
            String message,
            LocalDateTime responseTime
    ) {}
}
