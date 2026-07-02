package com.smartfarm.simulator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfarm.business.alert.AlertEngine;
import com.smartfarm.business.entity.Device;
import com.smartfarm.business.entity.SensorData;
import com.smartfarm.business.mapper.DeviceMapper;
import com.smartfarm.business.mapper.SensorDataMapper;
import com.smartfarm.framework.websocket.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 模拟数据采集器 — 动态轮询 + 批量插入 + 昼夜节律平滑曲线
 *
 * <pre>
 * v2.2 路演优化：
 *   1. 温度 = 基准值 + 昼夜正弦节律(±3°C, 峰值14:00/谷值4:00)
 *            + 小幅度随机噪声(±0.3°C)
 *            + 慢速趋势漂移(每小时±0.02°C)
 *   2. 湿度与温度负相关 + 随机噪声(±1%)
 *   3. 光照 = 正弦节律(白天高/夜间0) + 云层随机遮挡
 *   4. 土壤湿度 = 缓慢下降(蒸发) + 偶尔上升(模拟降雨/灌溉)
 *
 *   效果：ECharts 折线图呈现平滑正弦曲线，适合路演展示
 *
 * v2.1 修复：
 *   1. 使用 saveBatch 批量插入，严禁for循环单条insert
 *   2. 由 SchedulingConfig 提供的10线程池调度
 *
 * 动态轮询：绝不硬编码设备ID，每次从DB查询活跃SENSOR列表
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "data.collector", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockDataCollector extends ServiceImpl<SensorDataMapper, SensorData>
        implements DataCollector {

    private final DeviceMapper deviceMapper;
    private final WsPushService wsPushService;
    private final AlertEngine alertEngine;

    private final Random random = new Random();

    /** 上一次生成的数据基准值 — 用于模拟连续平滑变化 */
    private final java.util.Map<Long, MockDataSnapshot> snapshotMap =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void start() {
        log.info("[MockDataCollector] 模拟数据采集器已启动 — 动态轮询 + 批量插入模式, 间隔5秒");
    }

    @Override
    public void stop() {
        log.info("[MockDataCollector] 模拟数据采集器已停止");
    }

    @Override
    public String getName() {
        return "MockDataCollector";
    }

    /**
     * 定时任务: 每5秒执行一次（由 SchedulingConfig 10线程池调度）
     */
    @Scheduled(fixedRateString = "${data.mock.interval:5000}")
    public void generateMockData() {
        // ==================== 第一步：动态发现活跃SENSOR ====================
        List<Device> activeSensors = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getStatus, "ONLINE")
                        .eq(Device::getDeviceCategory, "SENSOR")
        );

        if (activeSensors.isEmpty()) {
            log.debug("[MockDataCollector] 当前无在线SENSOR设备，跳过数据生成");
            return;
        }

        log.debug("[MockDataCollector] 发现 {} 个在线SENSOR", activeSensors.size());

        LocalDateTime now = LocalDateTime.now();

        // ==================== 第二步：生成所有模拟数据 → 收集到List ====================
        List<SensorData> batchList = new ArrayList<>();

        for (Device sensor : activeSensors) {
            try {
                List<SensorData> deviceDataList = generateSensorDataList(sensor, now);
                batchList.addAll(deviceDataList);
            } catch (Exception e) {
                log.error("[MockDataCollector] 设备{}数据生成失败: {}",
                        sensor.getDeviceCode(), e.getMessage());
            }
        }

        // ==================== 第三步：saveBatch 批量写入 ====================
        // 关键修复：严禁在for循环中单条insert！
        // saveBatch 底层使用 JDBC Batch，一次网络往返完成所有INSERT
        if (!batchList.isEmpty()) {
            try {
                boolean saved = this.saveBatch(batchList, batchList.size());
                if (saved) {
                    log.debug("[MockDataCollector] 批量插入 {} 条传感器数据成功", batchList.size());
                }
            } catch (Exception e) {
                log.error("[MockDataCollector] 批量插入失败: {}", e.getMessage(), e);
                // 降级：逐条重试（确保数据不丢失）
                for (SensorData data : batchList) {
                    try {
                        this.save(data);
                    } catch (Exception ex) {
                        log.error("[MockDataCollector] 单条重试失败 deviceId={}", data.getDeviceId());
                    }
                }
            }
        }

        // ==================== 第四步：逐条WebSocket推送 ====================
        // WebSocket不支持批量发送，逐条推送
        for (SensorData data : batchList) {
            Device sensor = activeSensors.stream()
                    .filter(d -> d.getId().equals(data.getDeviceId()))
                    .findFirst().orElse(null);
            if (sensor != null && sensor.getPlotId() != null) {
                wsPushService.sendSensorData(sensor.getPlotId(), data.getDeviceId(),
                        data.getDataType(), data.getDataValue(), data.getUnit(),
                        data.getCollectTime(), data.getSource());
            }
        }

        // ==================== 第五步：更新设备心跳 ====================
        // 每次采集数据即视为一次"心跳"，防止 HeartbeatChecker 误判离线
        LocalDateTime heartbeatTime = LocalDateTime.now();
        for (Device sensor : activeSensors) {
            sensor.setLastHeartbeat(heartbeatTime);
            if (!"ONLINE".equals(sensor.getStatus())) {
                sensor.setStatus("ONLINE");
            }
            deviceMapper.updateById(sensor);
        }

        // ==================== 第六步：异步触发告警扫描 ====================
        // 告警引擎在其他线程执行，不阻塞下次数据采集
        alertEngine.scan(activeSensors);
    }

    /**
     * 为指定SENSOR生成模拟数据列表。
     * <p>
     * 核心算法：基准值 + 昼夜正弦节律 + 微小随机噪声 + 慢速趋势漂移。
     * 保证相邻两次采样的数值变化平滑连续，ECharts 折线图呈现自然波动曲线。
     */
    private List<SensorData> generateSensorDataList(Device sensor, LocalDateTime collectTime) {
        List<SensorData> dataList = new ArrayList<>();
        MockDataSnapshot snapshot = snapshotMap.computeIfAbsent(
                sensor.getId(), k -> MockDataSnapshot.createDefault(sensor.getDeviceType()));

        // 计算昼夜节律因子（基于当前小时，正弦波：峰值14:00，谷值4:00）
        int hour = collectTime.getHour();
        double diurnalFactor = Math.sin((hour - 4) * Math.PI / 14.0); // [-1, 1]

        switch (sensor.getDeviceType()) {
            case "TEMP_HUMIDITY" -> {
                // 温度 = 基准 + 昼夜节律(±3°C) + 随机噪声(±0.3°C) + 趋势漂移
                double tempBase = snapshot.temperatureBase;
                double diurnalEffect = diurnalFactor * 3.0;           // 昼夜振幅 ±3°C
                double noise = (random.nextGaussian()) * 0.3;          // 高斯噪声 σ=0.3°C
                double trend = snapshot.temperatureTrend;              // 慢速趋势

                double temp = tempBase + diurnalEffect + noise + trend;
                temp = clamp(temp, 15.0, 40.0);
                snapshot.temperatureTrend += (random.nextDouble() - 0.5) * 0.0005; // 趋势缓慢漂移

                // 湿度 = 基准 - 温度偏移×1.5(负相关) + 随机噪声(±1%) + 趋势
                double humBase = snapshot.humidityBase;
                double tempOffset = (temp - tempBase) * 1.5;           // 温度升高→湿度降低
                double humNoise = (random.nextGaussian()) * 1.0;       // 高斯噪声 σ=1%
                double humTrend = snapshot.humidityTrend;

                double humidity = humBase - tempOffset + humNoise + humTrend;
                humidity = clamp(humidity, 20.0, 95.0);
                snapshot.humidityTrend += (random.nextDouble() - 0.5) * 0.001;

                dataList.add(buildSensorData(sensor.getId(), "TEMPERATURE",
                        BigDecimal.valueOf(round2(temp)), "°C", collectTime));
                dataList.add(buildSensorData(sensor.getId(), "HUMIDITY",
                        BigDecimal.valueOf(round2(humidity)), "%", collectTime));
            }
            case "SOIL_MOISTURE" -> {
                // 土壤湿度 = 缓慢蒸发下降 + 随机微小波动 + 偶尔模拟降雨(2%概率大幅上升)
                double moisture = snapshot.soilMoisture;
                double evaporation = 0.02;                               // 基础蒸发速率
                double noise = (random.nextGaussian()) * 0.1;            // 微小波动
                boolean rainEvent = random.nextDouble() < 0.02;          // 2%概率模拟灌溉/降雨

                if (rainEvent) {
                    moisture += 3.0 + random.nextDouble() * 5.0;         // 降雨: +3~8%
                } else {
                    moisture -= evaporation + noise;
                }
                moisture = clamp(moisture, 15.0, 85.0);
                snapshot.soilMoisture = moisture;

                dataList.add(buildSensorData(sensor.getId(), "SOIL_MOISTURE",
                        BigDecimal.valueOf(round2(moisture)), "%", collectTime));
            }
            case "LIGHT" -> {
                // 光照 = 正弦节律(白天) + 云层随机遮挡(30%概率衰减20-60%)
                double baseLight = diurnalFactor > -0.2
                        ? 1800 * Math.max(0, diurnalFactor)              // 白天：正弦曲线 0~1800 lux
                        : 0;                                             // 夜间：0 lux
                boolean cloudy = random.nextDouble() < 0.3;
                if (cloudy) {
                    baseLight *= (0.4 + random.nextDouble() * 0.4);      // 云层: 衰减到40%~80%
                }
                double lightNoise = (random.nextGaussian()) * 30;        // 噪声 σ=30lux
                double light = Math.max(0, baseLight + lightNoise);
                light = clamp(light, 0, 2000);

                dataList.add(buildSensorData(sensor.getId(), "LIGHT_INTENSITY",
                        BigDecimal.valueOf(round2(light)), "lux", collectTime));
            }
            default -> log.debug("[MockDataCollector] 未知设备类型: {} ({})",
                    sensor.getDeviceType(), sensor.getDeviceCode());
        }

        return dataList;
    }

    private SensorData buildSensorData(Long deviceId, String dataType,
                                        BigDecimal value, String unit,
                                        LocalDateTime collectTime) {
        return SensorData.builder()
                .deviceId(deviceId)
                .dataType(dataType)
                .dataValue(value)
                .unit(unit)
                .collectTime(collectTime)
                .source("MOCK")
                .build();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 模拟数据快照 — 保存基准值和趋势用于产生平滑连续曲线
     * <p>
     * 设计思路：
     *   - base: 初始基准值（设备安装时的典型值），用于昼夜节律的中点
     *   - trend: 慢速漂移累积量，模拟天气系统缓慢变化（如冷锋过境）
     *   - soilMoisture: 土壤湿度不用base+trend模型，直接用上一时刻值+蒸发
     */
    private static class MockDataSnapshot {
        /** 温度基准值（昼夜节律的中点温度） */
        double temperatureBase;
        /** 温度慢速趋势累积 */
        double temperatureTrend;
        /** 湿度基准值 */
        double humidityBase;
        /** 湿度慢速趋势累积 */
        double humidityTrend;
        /** 土壤湿度（上一时刻值） */
        double soilMoisture;

        static MockDataSnapshot createDefault(String deviceType) {
            MockDataSnapshot snap = new MockDataSnapshot();
            // 地块A（温室大棚）：温度适中稳定，湿度较高
            // 地块B（露天农田）：温度稍高波动大，湿度偏低
            if (deviceType != null && deviceType.contains("TEMP")) {
                snap.temperatureBase = 25.0 + Math.random() * 4;  // 25~29°C 随机基准
                snap.humidityBase = 60.0 + Math.random() * 15;     // 60~75% 随机基准
            } else {
                snap.temperatureBase = 26.0;
                snap.humidityBase = 65.0;
            }
            snap.temperatureTrend = 0.0;
            snap.humidityTrend = 0.0;
            snap.soilMoisture = 55.0 + Math.random() * 15;         // 55~70%
            return snap;
        }
    }
}
