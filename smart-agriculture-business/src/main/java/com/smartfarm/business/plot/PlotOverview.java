package com.smartfarm.business.plot;

import com.smartfarm.business.entity.Device;
import com.smartfarm.business.entity.Plot;
import com.smartfarm.business.entity.SensorData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 地块综合概览VO — 聚合返回给前端
 * <p>
 * 包含：
 *   - 地块基本信息
 *   - SENSOR设备列表
 *   - CONTROLLER设备列表（前端用于判断是否渲染控制面板）
 *   - 每个SENSOR的最新数据
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlotOverview {

    /** 地块基本信息 */
    private Plot plot;

    /** SENSOR类设备列表 */
    private List<Device> sensors;

    /** CONTROLLER类设备列表 — 前端据此判断是否显示空状态 */
    private List<Device> controllers;

    /** 每个SENSOR设备的最新一条传感器数据 (deviceId → SensorData) */
    private Map<Long, SensorData> latestSensorData;

    /**
     * 该地块是否有控制设备 — 前端可直接使用此布尔值
     */
    public boolean hasControllers() {
        return controllers != null && !controllers.isEmpty();
    }

    /**
     * 该地块是否有传感器设备
     */
    public boolean hasSensors() {
        return sensors != null && !sensors.isEmpty();
    }
}
