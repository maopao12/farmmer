package com.smartfarm.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警记录实体
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("alert_log")
public class AlertLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联规则ID */
    private Long ruleId;

    /** 设备ID */
    private Long deviceId;

    /** 地块ID */
    private Long plotId;

    /** 告警内容 */
    private String alertMsg;

    /** 告警级别: CRITICAL / WARNING / INFO */
    private String alertLevel;

    /** 触发时的实际值 */
    private BigDecimal currentValue;

    /** 已读标志: 0未读 1已读 */
    private Integer isRead;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
