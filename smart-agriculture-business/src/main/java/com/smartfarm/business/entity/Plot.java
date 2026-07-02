package com.smartfarm.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 地块实体
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("plot")
public class Plot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 地块名称 */
    private String name;

    /** 地理位置 */
    private String location;

    /** 面积(亩) */
    private BigDecimal area;

    /** 种植作物 */
    private String cropType;

    /** 归属农户ID */
    private Long ownerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
