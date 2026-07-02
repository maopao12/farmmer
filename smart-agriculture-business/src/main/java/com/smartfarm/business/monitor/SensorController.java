package com.smartfarm.business.monitor;

import com.smartfarm.business.entity.SensorData;
import com.smartfarm.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器数据控制器
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    /** 获取设备最新传感器数据 */
    @GetMapping("/realtime/{deviceId}")
    public R<SensorData> realtime(@PathVariable Long deviceId) {
        return R.ok(sensorService.getRealtimeData(deviceId));
    }

    /**
     * 历史数据查询 — 自动降采样
     * @param deviceId 设备ID
     * @param dataType 数据类型 (TEMPERATURE/HUMIDITY/SOIL_MOISTURE/LIGHT_INTENSITY)
     * @param days 查询天数 (默认7)
     * @param startTime 开始时间（可选，与days二选一）
     * @param endTime 结束时间（可选）
     */
    @GetMapping("/history")
    public R<List<SensorService.DownsampledPoint>> history(
            @RequestParam("deviceId") Long deviceId,
            @RequestParam(name = "dataType", required = false) String dataType,
            @RequestParam(name = "days", defaultValue = "7") int days,
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(sensorService.getHistoryData(deviceId, dataType, days, startTime, endTime));
    }
}
