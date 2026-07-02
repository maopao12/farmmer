package com.smartfarm.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备实体 — 核心区分 device_category (SENSOR / CONTROLLER)
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备唯一编码 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型: TEMP_HUMIDITY / SOIL_MOISTURE / IRRIGATION / LIGHT / VENTILATION */
    private String deviceType;

    /**
     * 设备职能分类 — 核心字段
     * SENSOR: 传感器(温度、湿度、光照、CO2等)——只采集数据,不支持控制
     * CONTROLLER: 控制器(灌溉、通风等)——可接收并执行控制指令
     */
    private String deviceCategory;

    /** 绑定地块ID (NULL = 仓库中未绑定) */
    private Long plotId;

    /** 状态: ONLINE / OFFLINE / FAULT */
    private String status;

    /** MQTT订阅/发布Topic */
    private String mqttTopic;

    /** 安装位置描述 */
    private String installLocation;

    /** 最后心跳时间 — 用于离线判定(3分钟无心跳→OFFLINE) */
    private LocalDateTime lastHeartbeat;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
