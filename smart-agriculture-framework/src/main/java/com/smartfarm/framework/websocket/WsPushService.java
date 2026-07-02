package com.smartfarm.framework.websocket;

import com.smartfarm.common.event.WsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WebSocket推送服务 — 统一管理所有Topic的消息推送
 *
 * <pre>
 * Topic规范:
 *   /topic/plot/{plotId}/sensors  — 传感器实时数据
 *   /topic/plot/{plotId}/alerts   — 告警通知
 *   /topic/device/{id}/status     — 设备状态 + 指令结果
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsPushService {

    private final SimpMessagingTemplate messagingTemplate;

    // ==================== 传感器数据推送 ====================

    public void sendSensorData(Long plotId, Long deviceId, String dataType,
                                BigDecimal value, String unit,
                                LocalDateTime collectTime, String source) {
        WsEvent.SensorDataEvent event = new WsEvent.SensorDataEvent(
                deviceId, dataType, value, unit, collectTime, source);
        String destination = "/topic/plot/" + plotId + "/sensors";
        messagingTemplate.convertAndSend(destination, event);
    }

    // ==================== 告警推送 ====================

    public void sendAlert(Long plotId, Long alertId, Long deviceId,
                           String alertMsg, String alertLevel,
                           BigDecimal currentValue, LocalDateTime triggerTime) {
        WsEvent.AlertEvent event = new WsEvent.AlertEvent(
                alertId, deviceId, plotId, alertMsg, alertLevel,
                currentValue, triggerTime);
        String destination = "/topic/plot/" + plotId + "/alerts";
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WS推送] 告警 → {}", destination);
    }

    // ==================== 设备状态变更 ====================

    public void sendDeviceStatusChange(Long deviceId, String newStatus) {
        WsEvent.DeviceStatusEvent event = new WsEvent.DeviceStatusEvent(deviceId, newStatus);
        String destination = "/topic/device/" + deviceId + "/status";
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WS推送] 设备{}状态 → {}", deviceId, newStatus);
    }

    // ==================== 指令执行结果 ====================

    public void sendCommandResult(Long deviceId, String command,
                                   String status, String message,
                                   LocalDateTime responseTime) {
        WsEvent.CommandResultEvent event = new WsEvent.CommandResultEvent(
                deviceId, command, status, message, responseTime);
        String destination = "/topic/device/" + deviceId + "/status";
        messagingTemplate.convertAndSend(destination, event);
        log.info("[WS推送] 指令结果 device={}, status={}", deviceId, status);
    }
}
