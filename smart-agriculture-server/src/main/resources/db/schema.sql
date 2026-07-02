-- ============================================================
-- 智慧农业系统 - 数据库建表脚本 (schema.sql)
-- 版本: v2.0 | 日期: 2026-07-01
-- 数据库: smart_agriculture
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_agriculture
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_agriculture;

-- ============================================================
-- 1. user (用户表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `real_name`   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色: FARMER/ADMIN/SUPER_ADMIN',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `enabled`     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态: 1启用 0禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. plot (地块表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `plot` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '地块ID',
    `name`        VARCHAR(100)  NOT NULL COMMENT '地块名称',
    `location`    VARCHAR(255)  DEFAULT NULL COMMENT '地理位置',
    `area`        DECIMAL(10,2) DEFAULT NULL COMMENT '面积(亩)',
    `crop_type`   VARCHAR(50)   DEFAULT NULL COMMENT '种植作物',
    `owner_id`    BIGINT        NOT NULL COMMENT '归属农户ID',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_plot_owner` (`owner_id`),
    CONSTRAINT `fk_plot_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地块表';

-- ============================================================
-- 3. device (设备表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `device` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `device_code`      VARCHAR(100) NOT NULL COMMENT '设备唯一编码',
    `device_name`      VARCHAR(100) NOT NULL COMMENT '设备名称',
    `device_type`      VARCHAR(50)  NOT NULL COMMENT '设备类型: TEMP_HUMIDITY/SOIL_MOISTURE/IRRIGATION/LIGHT/VENTILATION',
    `device_category`  VARCHAR(20)  NOT NULL COMMENT '设备职能: SENSOR/CONTROLLER',
    `plot_id`          BIGINT       DEFAULT NULL COMMENT '绑定地块ID(NULL=仓库中)',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE' COMMENT '状态: ONLINE/OFFLINE/FAULT',
    `mqtt_topic`       VARCHAR(255) DEFAULT NULL COMMENT 'MQTT订阅/发布Topic',
    `install_location` VARCHAR(255) DEFAULT NULL COMMENT '安装位置描述',
    `last_heartbeat`   DATETIME     DEFAULT NULL COMMENT '最后心跳时间',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`device_code`),
    KEY `idx_device_plot` (`plot_id`),
    KEY `idx_device_category` (`device_category`),
    KEY `idx_device_status` (`status`),
    CONSTRAINT `fk_device_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

-- ============================================================
-- 4. sensor_data (传感器数据表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `sensor_data` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '数据ID',
    `device_id`    BIGINT        NOT NULL COMMENT '设备ID',
    `data_type`    VARCHAR(50)   NOT NULL COMMENT '数据类型: TEMPERATURE/HUMIDITY/SOIL_MOISTURE/LIGHT_INTENSITY/CO2',
    `data_value`   DECIMAL(10,2) NOT NULL COMMENT '采集值',
    `unit`         VARCHAR(20)   DEFAULT NULL COMMENT '单位: °C/%/lux/ppm',
    `collect_time` DATETIME      NOT NULL COMMENT '采集时间',
    `source`       VARCHAR(10)   NOT NULL DEFAULT 'MOCK' COMMENT '数据来源: MOCK/MQTT',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sensor_device` (`device_id`),
    KEY `idx_sensor_type_time` (`data_type`, `collect_time`),
    KEY `idx_sensor_collect_time` (`collect_time`),
    CONSTRAINT `fk_sensor_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器数据表';

-- ============================================================
-- 5. alert_rule (告警规则表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `device_id`     BIGINT        DEFAULT NULL COMMENT '设备ID(NULL=全局规则)',
    `plot_id`       BIGINT        DEFAULT NULL COMMENT '地块ID',
    `metric_type`   VARCHAR(50)   NOT NULL COMMENT '监控指标类型',
    `rule_name`     VARCHAR(100)  NOT NULL COMMENT '规则名称',
    `min_threshold` DECIMAL(10,2) DEFAULT NULL COMMENT '下限阈值',
    `max_threshold` DECIMAL(10,2) DEFAULT NULL COMMENT '上限阈值',
    `alert_level`   VARCHAR(20)   NOT NULL DEFAULT 'WARNING' COMMENT '告警级别: CRITICAL/WARNING/INFO',
    `enabled`       TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '启用状态',
    `notify_method` VARCHAR(50)   DEFAULT 'WEBSOCKET' COMMENT '通知方式: WEBSOCKET/SMS',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_alert_rule_device` (`device_id`),
    KEY `idx_alert_rule_plot` (`plot_id`),
    CONSTRAINT `fk_alert_rule_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_alert_rule_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- ============================================================
-- 6. alert_log (告警记录表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '告警日志ID',
    `rule_id`       BIGINT       DEFAULT NULL COMMENT '关联规则ID',
    `device_id`     BIGINT       DEFAULT NULL COMMENT '设备ID',
    `plot_id`       BIGINT       DEFAULT NULL COMMENT '地块ID',
    `alert_msg`     VARCHAR(500) NOT NULL COMMENT '告警内容',
    `alert_level`   VARCHAR(20)  NOT NULL COMMENT '告警级别: CRITICAL/WARNING/INFO',
    `current_value` DECIMAL(10,2) DEFAULT NULL COMMENT '触发时的实际值',
    `is_read`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '已读标志: 0未读 1已读',
    `trigger_time`  DATETIME     NOT NULL COMMENT '触发时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_alert_log_device` (`device_id`),
    KEY `idx_alert_log_plot` (`plot_id`),
    KEY `idx_alert_log_trigger_time` (`trigger_time`),
    CONSTRAINT `fk_alert_log_rule` FOREIGN KEY (`rule_id`) REFERENCES `alert_rule` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_alert_log_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_alert_log_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';

-- ============================================================
-- 7. control_log (控制指令日志表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `control_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `device_id`      BIGINT       NOT NULL COMMENT '目标设备ID',
    `plot_id`        BIGINT       DEFAULT NULL COMMENT '地块ID',
    `operator_id`    BIGINT       NOT NULL COMMENT '操作人ID',
    `command`        VARCHAR(50)  NOT NULL COMMENT '指令: ON/OFF/SET_PARAM',
    `command_params` VARCHAR(255) DEFAULT NULL COMMENT '指令参数(JSON)',
    `command_status` VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SENT/SUCCESS/FAILED/TIMEOUT',
    `result_msg`     TEXT         DEFAULT NULL COMMENT '执行结果或失败原因',
    `send_time`      DATETIME     NOT NULL COMMENT '指令发送时间',
    `response_time`  DATETIME     DEFAULT NULL COMMENT '设备响应时间',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_control_log_device` (`device_id`),
    KEY `idx_control_log_operator` (`operator_id`),
    KEY `idx_control_log_status` (`command_status`),
    KEY `idx_control_log_send_time` (`send_time`),
    CONSTRAINT `fk_control_log_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_control_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='控制指令日志表';

-- ============================================================
-- 8. knowledge_base (知识库表)
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识ID',
    `question`    VARCHAR(500) NOT NULL COMMENT '标准问题',
    `answer`      TEXT         NOT NULL COMMENT '标准答案',
    `category`    VARCHAR(50)  DEFAULT NULL COMMENT '分类: IRRIGATION/FERTILIZER/PEST/DISEASE/GENERAL',
    `keywords`    VARCHAR(255) DEFAULT NULL COMMENT '关键词(逗号分隔)',
    `crop_type`   VARCHAR(50)  DEFAULT NULL COMMENT '关联作物',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    FULLTEXT KEY `ft_knowledge` (`question`, `answer`, `keywords`)  -- MySQL全文索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';
