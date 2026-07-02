-- ============================================================
-- 智慧农业系统 - 预置初始化数据 (data.sql)
-- 版本: v2.0 | 日期: 2026-07-01
-- 所有BCrypt密码均为明文对应值(见注释)，由后端启动时加密
-- ============================================================

USE smart_agriculture;

-- ============================================================
-- 1. 用户初始化 (BCrypt加密后的密码)
--    明文: admin123  → $2b$10$7F.t.5Q9ygURAx.TZSxrNusuZsr5O8OcmFdGzn5iS41.KjnJDQhVO
--    明文: farmer123 → $2b$10$7F.t.5Q9ygURAx.TZSxrNusuZsr5O8OcmFdGzn5iS41.KjnJDQhVO
--    实际部署时应使用BCryptPasswordEncoder重新生成
-- ============================================================
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `role`, `phone`, `enabled`) VALUES
(1, 'admin',
 '$2b$10$7F.t.5Q9ygURAx.TZSxrNusuZsr5O8OcmFdGzn5iS41.KjnJDQhVO',
 '系统管理员', 'ADMIN', '13800000000', 1),
(2, 'farmer1',
 '$2b$10$BzpEIlUHrWAuKfZcD8UBy.obCiwxBonHtHAW1A8sdME92tTQ88Lqq',
 '农户老王', 'FARMER', '13800000001', 1),
(3, 'farmer2',
 '$2b$10$BzpEIlUHrWAuKfZcD8UBy.obCiwxBonHtHAW1A8sdME92tTQ88Lqq',
 '农户老李', 'FARMER', '13800000002', 1);

-- ============================================================
-- 2. 地块初始化
--    地块A → farmer1(农户老王), 地块B → farmer2(农户老李)
-- ============================================================
INSERT INTO `plot` (`id`, `name`, `location`, `area`, `crop_type`, `owner_id`) VALUES
(1, '地块A · 温室大棚', '重庆市江津区现代农业园区东区', 2.50, '番茄', 2),
(2, '地块B · 露天农田', '重庆市江津区现代农业园区西区', 5.00, '水稻', 3);

-- ============================================================
-- 3. 设备初始化 — 严格按场景配置
--
--    ┌──────────────┬──────────────────┬──────────┬───────────┐
--    │ 地块         │ SENSOR           │ CONTROLLER│ 状态      │
--    ├──────────────┼──────────────────┼───────────┼───────────┤
--    │ 地块A (id=1) │ DEV-TH-001       │ DEV-IR-001│ 均有      │
--    │ 地块B (id=2) │ DEV-TH-002       │ (无)      │ 仅SENSOR  │
--    │ 仓库(NULL)   │ (无)             │ DEV-IR-002│ 待绑定    │
--    └──────────────┴──────────────────┴───────────┴───────────┘
--
--    验证场景:
--    - 地块A: 有SENSOR + CONTROLLER → 灌溉按钮正常 → 控制功能正常
--    - 地块B: 仅SENSOR, 无CONTROLLER → 空状态提示
--    - 仓库: DEV-IR-002未绑定 → 管理员可分配
-- ============================================================
INSERT INTO `device` (`id`, `device_code`, `device_name`, `device_type`, `device_category`, `plot_id`, `status`, `mqtt_topic`, `install_location`, `last_heartbeat`) VALUES
-- 地块A设备
(1, 'DEV-TH-001', '温湿度传感器 #1', 'TEMP_HUMIDITY', 'SENSOR',      1, 'ONLINE',  'sensor/plot-a/th001/data',   '地块A-中央立柱',   NOW()),
(2, 'DEV-IR-001', '灌溉控制器 #1',     'IRRIGATION',    'CONTROLLER', 1, 'ONLINE',  'device/plot-a/ir001/control', '地块A-东区灌溉口', NOW()),
-- 地块B设备 (仅SENSOR，无CONTROLLER)
(3, 'DEV-TH-002', '温湿度传感器 #2', 'TEMP_HUMIDITY', 'SENSOR',      2, 'ONLINE',  'sensor/plot-b/th002/data',   '地块B-田埂中部',   NOW()),
-- 仓库中的未绑定CONTROLLER
(4, 'DEV-IR-002', '灌溉控制器 #2',     'IRRIGATION',    'CONTROLLER', NULL, 'OFFLINE', 'device/plot-b/ir002/control', NULL,            NULL);

-- ============================================================
-- 4. 传感器模拟初始数据 (为每个SENSOR造一条最新数据用于演示)
-- ============================================================
INSERT INTO `sensor_data` (`device_id`, `data_type`, `data_value`, `unit`, `collect_time`, `source`) VALUES
-- DEV-TH-001 (地块A温湿度)
(1, 'TEMPERATURE',    26.5, '°C',  NOW(), 'MOCK'),
(1, 'HUMIDITY',       68.0, '%',   NOW(), 'MOCK'),
(1, 'LIGHT_INTENSITY', 850, 'lux', NOW(), 'MOCK'),
-- DEV-TH-002 (地块B温湿度)
(3, 'TEMPERATURE',    31.2, '°C',  NOW(), 'MOCK'),
(3, 'HUMIDITY',       55.0, '%',   NOW(), 'MOCK');

-- ============================================================
-- 5. 告警规则预置 (为每个SENSOR建默认规则)
-- ============================================================
INSERT INTO `alert_rule` (`device_id`, `plot_id`, `metric_type`, `rule_name`, `min_threshold`, `max_threshold`, `alert_level`, `enabled`, `notify_method`) VALUES
-- 地块A · 温湿度传感器规则
(1, 1, 'HUMIDITY',        '土壤湿度过低告警', 30.0, NULL, 'CRITICAL', 1, 'WEBSOCKET'),
(1, 1, 'TEMPERATURE',     '温度过高告警',     NULL, 40.0, 'CRITICAL', 1, 'WEBSOCKET'),
(1, 1, 'HUMIDITY',        '土壤湿度预警',     45.0, NULL, 'INFO',     1, 'WEBSOCKET'),
-- 地块B · 温湿度传感器规则
(3, 2, 'HUMIDITY',        '土壤湿度过低告警', 25.0, NULL, 'CRITICAL', 1, 'WEBSOCKET'),
(3, 2, 'TEMPERATURE',     '温度过高告警',     NULL, 42.0, 'CRITICAL', 1, 'WEBSOCKET');

-- ============================================================
-- 6. 知识库预置数据 (5条示例)
-- ============================================================
INSERT INTO `knowledge_base` (`question`, `answer`, `category`, `keywords`, `crop_type`) VALUES
('番茄苗期湿度多少合适？',
 '番茄苗期土壤湿度应保持在60%-80%之间。湿度过高(>85%)容易导致根腐病，湿度过低(<45%)会影响幼苗正常生长。建议采用滴灌方式，少量多次浇水，保持土壤湿润但不积水。同时注意观察叶片状态，如出现萎蔫说明需要及时补水。',
 'IRRIGATION', '番茄,苗期,湿度,浇水,土壤', '番茄'),

('水稻分蘖期需要多少水？',
 '水稻分蘖期需要保持3-5cm的浅水层。水层过深会抑制分蘖，水层过浅或晒田过度则影响养分吸收。分蘖后期可适当晒田2-3天，促进根系发育。注意：分蘖期是水稻需水关键期，但并非水越多越好，应保持"浅水勤灌"的原则。',
 'IRRIGATION', '水稻,分蘖,浇水,水层', '水稻'),

('土壤湿度过低怎么办？',
 '土壤湿度过低时应立即采取以下措施：1) 开启灌溉系统，建议使用滴灌或喷灌方式避免大水漫灌；2) 灌溉时间建议选择早晨或傍晚以减少蒸发；3) 检查土壤是否板结，必要时松土增加保水能力；4) 可在地表覆盖稻草或地膜减少水分蒸发。对于已经受旱的作物，应分次灌溉，避免一次性大量浇水造成根系损伤。',
 'IRRIGATION', '湿度,过低,灌溉,干旱,浇水', NULL),

('如何判断作物是否缺水？',
 '判断作物缺水可以从以下几个方面观察：1) 叶片状态：叶片萎蔫、卷曲、颜色变暗是缺水典型症状；2) 土壤判断：取地表下5-10cm土壤，手握不成团或松手即散说明缺水；3) 生长速度：缺水时作物生长明显减缓，新叶小而薄；4) 传感器数据：土壤湿度传感器读数持续低于40%时需注意补水。建议结合多种方法综合判断。',
 'GENERAL', '缺水,判断,叶片,土壤,干旱', NULL),

('温室大棚温度过高如何处理？',
 '温室大棚温度过高（>35°C）时处理方法：1) 立即开启通风设备（天窗、侧窗），增加空气对流；2) 开启遮阳网，减少阳光直射；3) 启动湿帘-风机降温系统（如有配置）；4) 适当喷雾增加空气湿度辅助降温；5) 注意：温度骤降幅度不宜超过5°C/h，避免作物产生应激反应。高温天气建议提前做好预防，而非等温度过高再处理。',
 'GENERAL', '温室,温度,过高,通风,降温,大棚', '番茄');
