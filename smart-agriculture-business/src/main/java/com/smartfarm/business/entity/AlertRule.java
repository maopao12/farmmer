package com.smartfarm.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则实体
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("alert_rule")
public class AlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID (NULL=全局规则) */
    private Long deviceId;

    /** 地块ID */
    private Long plotId;

    /** 监控指标类型 */
    private String metricType;

    /** 规则名称 */
    private String ruleName;

    /** 下限阈值 */
    private BigDecimal minThreshold;

    /** 上限阈值 */
    private BigDecimal maxThreshold;

    /** 告警级别: CRITICAL / WARNING / INFO */
    private String alertLevel;

    /** 启用状态 */
    private Integer enabled;

    /** 通知方式: WEBSOCKET / SMS */
    private String notifyMethod;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
