package com.smartfarm.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 传感器数据实体
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sensor_data")
public class SensorData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    private Long deviceId;

    /** 数据类型: TEMPERATURE / HUMIDITY / SOIL_MOISTURE / LIGHT_INTENSITY / CO2 */
    private String dataType;

    /** 采集值 */
    private BigDecimal dataValue;

    /** 单位: °C / % / lux / ppm */
    private String unit;

    /** 采集时间 */
    private LocalDateTime collectTime;

    /** 数据来源: MOCK / MQTT */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
