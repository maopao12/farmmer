package com.smartfarm.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 控制指令日志实体 — 所有操作行为强制审计
 * <p>
 * 状态流转: PENDING → SENT → SUCCESS / FAILED / TIMEOUT
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("control_log")
public class ControlLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标设备ID */
    private Long deviceId;

    /** 地块ID */
    private Long plotId;

    /** 操作人ID */
    private Long operatorId;

    /** 指令: ON / OFF / SET_PARAM */
    private String command;

    /** 指令参数(JSON)，如定时灌溉时长 */
    private String commandParams;

    /**
     * 指令状态:
     * PENDING  - 已接收,待发送
     * SENT     - 已发送到MQTT,等待设备响应
     * SUCCESS  - 设备成功响应
     * FAILED   - 设备返回失败
     * TIMEOUT  - 10秒超时无响应
     */
    private String commandStatus;

    /** 执行结果或失败原因 */
    private String resultMsg;

    /** 指令发送时间 */
    private LocalDateTime sendTime;

    /** 设备响应时间 (SUCCESS时有值) */
    private LocalDateTime responseTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
