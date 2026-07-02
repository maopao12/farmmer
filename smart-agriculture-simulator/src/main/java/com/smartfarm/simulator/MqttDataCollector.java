package com.smartfarm.simulator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.Device;
import com.smartfarm.business.entity.SensorData;
import com.smartfarm.business.mapper.DeviceMapper;
import com.smartfarm.business.mapper.SensorDataMapper;
import com.smartfarm.framework.websocket.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MQTT 数据采集器 — 接收真实硬件传感器数据
 *
 * <pre>
 * 数据链路:
 *   硬件传感器 → MQTT Broker → 本采集器接收 → 写入 sensor_data 表
 *                                         → 更新 device.last_heartbeat
 *                                         → WebSocket 推送给前端
 *
 * 与 MockDataCollector 的关系:
 *   两者都实现 DataCollector 接口，通过 application.yml 的
 *   data.collector 配置切换（mock / mqtt），业务代码零改动。
 *
 * 前端/API 层完全无感知 —— 数据来源变了，但查询的 SQL 和表都一样。
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "data.collector", havingValue = "mqtt")
@RequiredArgsConstructor
public class MqttDataCollector implements DataCollector {

    private final DeviceMapper deviceMapper;
    private final SensorDataMapper sensorDataMapper;
    private final WsPushService wsPushService;

    @Override
    public void start() {
        log.info("[MqttDataCollector] MQTT采集器已启动，等待硬件数据...");
        // TODO: 基地阶段配置 MQTT 连接
        // mqttClient.connect(brokerUrl, clientId);
        // mqttClient.subscribe("sensor/+/data", this::onSensorData);
        // mqttClient.subscribe("device/+/heartbeat", this::onHeartbeat);
        // mqttClient.subscribe("device/+/response", this::onDeviceResponse);
    }

    @Override
    public void stop() {
        log.info("[MqttDataCollector] MQTT采集器已停止");
    }

    @Override
    public String getName() {
        return "MqttDataCollector";
    }

    /**
     * 接收传感器数据报文（MQTT回调入口）
     *
     * <pre>
     * 报文格式 (JSON):
     * {
     *   "deviceCode": "DEV-TH-001",
     *   "dataType": "TEMPERATURE",
     *   "dataValue": 26.5,
     *   "unit": "°C",
     *   "timestamp": "2026-07-01 14:30:00"
     * }
     * </pre>
     *
     * @param topic   MQTT Topic (如 sensor/DEV-TH-001/data)
     * @param payload JSON 报文
     */
    public void onSensorData(String topic, String payload) {
        try {
            // 1. 解析报文
            // SensorMessage msg = JSON.parseObject(payload, SensorMessage.class);
            // 这里用伪代码表示实际逻辑，基地阶段接入真实MQTT客户端后替换
            String deviceCode = extractDeviceCode(topic);
            String dataType = extractField(payload, "dataType");
            BigDecimal dataValue = new BigDecimal(extractField(payload, "dataValue"));
            String unit = extractField(payload, "unit");

            // 2. 根据设备编码查找设备ID
            Device device = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>()
                            .eq(Device::getDeviceCode, deviceCode)
            );

            if (device == null) {
                log.warn("[MqttDataCollector] 未知设备: {}", deviceCode);
                return;
            }

            // 3. 写入传感器数据表
            SensorData data = SensorData.builder()
                    .deviceId(device.getId())
                    .dataType(dataType)
                    .dataValue(dataValue)
                    .unit(unit)
                    .collectTime(LocalDateTime.now())
                    .source("MQTT")
                    .build();
            sensorDataMapper.insert(data);

            // 4. 更新设备心跳时间（证明设备在线）
            device.setLastHeartbeat(LocalDateTime.now());
            if (!"ONLINE".equals(device.getStatus())) {
                device.setStatus("ONLINE");
                log.info("[MqttDataCollector] 设备{}恢复在线", device.getDeviceCode());
            }
            deviceMapper.updateById(device);

            // 5. WebSocket 实时推送给前端
            if (device.getPlotId() != null) {
                wsPushService.sendSensorData(device.getPlotId(), data.getDeviceId(),
                        data.getDataType(), data.getDataValue(), data.getUnit(),
                        data.getCollectTime(), data.getSource());
            }

            log.debug("[MqttDataCollector] 收到数据: {} {}={}{}",
                    deviceCode, dataType, dataValue, unit);

        } catch (Exception e) {
            log.error("[MqttDataCollector] 数据处理失败: topic={}, error={}", topic, e.getMessage());
        }
    }

    /**
     * 接收设备心跳（MQTT回调入口）
     *
     * <pre>
     * 报文格式: {"deviceCode": "DEV-TH-001", "timestamp": "..."}
     * </pre>
     */
    public void onHeartbeat(String topic, String payload) {
        try {
            String deviceCode = extractDeviceCode(topic);

            Device device = deviceMapper.selectOne(
                    new LambdaQueryWrapper<Device>()
                            .eq(Device::getDeviceCode, deviceCode)
            );

            if (device == null) return;

            // 更新心跳时间
            device.setLastHeartbeat(LocalDateTime.now());
            if (!"ONLINE".equals(device.getStatus())) {
                device.setStatus("ONLINE");
                wsPushService.sendDeviceStatusChange(device.getId(), "ONLINE");
                log.info("[MqttDataCollector] 设备{}恢复在线(心跳)", deviceCode);
            }
            deviceMapper.updateById(device);

        } catch (Exception e) {
            log.error("[MqttDataCollector] 心跳处理失败: {}", e.getMessage());
        }
    }

    // ==================== 报文解析辅助方法 ====================

    private String extractDeviceCode(String topic) {
        // topic 格式: sensor/DEV-TH-001/data → 取中间段
        String[] parts = topic.split("/");
        return parts.length >= 2 ? parts[1] : "UNKNOWN";
    }

    private String extractField(String payload, String fieldName) {
        // 简易JSON字段提取（生产环境用 Jackson/Gson 替换）
        String searchKey = "\"" + fieldName + "\"";
        int keyIdx = payload.indexOf(searchKey);
        if (keyIdx < 0) return "0";
        int colonIdx = payload.indexOf(":", keyIdx);
        int valueStart = colonIdx + 1;
        while (valueStart < payload.length() && payload.charAt(valueStart) == ' ') valueStart++;
        char firstChar = payload.charAt(valueStart);
        if (firstChar == '"') {
            int valueEnd = payload.indexOf("\"", valueStart + 1);
            return payload.substring(valueStart + 1, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < payload.length() && payload.charAt(valueEnd) != ',' && payload.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return payload.substring(valueStart, valueEnd).trim();
        }
    }
}
