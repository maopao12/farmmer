# 智慧农业系统 - 设计规格说明书 v2.0

> 重庆交通大学-中软国际 人才培养集中实训项目  
> 日期: 2026-07-01 | 阶段: 前15天校内阶段  
> 技术栈: JDK 21 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 + Vue 3 + uni-app

---

## 1. 项目概述

### 1.1 项目背景

智慧农业物联网平台，实现农田环境数据实时监测、设备远程控制、智能告警和AI农事问答。前15天使用模拟数据构建完整软件系统，后10天对接真实硬件（Hi3861/LiteOS）实现端-边-云一体化。

### 1.2 核心约束

- **模拟/真实切换零改动**：通过 `DataCollector` 接口抽象，配置切换 `mock`/`mqtt`，业务代码、API、数据库完全不变
- **双端联动**：PC端(Vue3)面向管理员，移动端(uni-app)面向农户，共用同一后端API和WebSocket通道
- **RBAC硬隔离**：所有SQL强制WHERE owner_id，农户间绝对数据隔离
- **前15天交付物**：完整可运行的全栈软件系统 + 模拟数据仿真 + 含预置数据可演示

---

## 2. 系统架构

### 2.1 技术架构

```
┌─────────────────────────────────────────────────┐
│  前端层                                            │
│  PC: Vue 3 + Element Plus + ECharts              │
│  Mobile: uni-app (H5/小程序/App)                    │
├─────────────────────────────────────────────────┤
│  通信层: REST API + WebSocket STOMP + SSE        │
├─────────────────────────────────────────────────┤
│  后端层: Spring Boot 3.2                          │
│  ┌──────────┬──────────┬──────────┬──────────┐  │
│  │ 监测模块  │ 控制模块  │ 告警模块  │ AI模块   │  │
│  │ 地块模块  │ 设备模块  │ 认证模块  │          │  │
│  └──────────┴──────────┴──────────┴──────────┘  │
├─────────────────────────────────────────────────┤
│  数据层: MySQL 8.0 + MyBatis-Plus 3.5            │
├─────────────────────────────────────────────────┤
│  数据采集层: DataCollector接口                     │
│  ├── MockDataCollector (校内-动态轮询)            │
│  └── MqttDataCollector (基地硬件)                 │
├─────────────────────────────────────────────────┤
│  消息与超时控制: 内嵌MQTT Broker + 指令响应闭       │
│  环(10s超时) + 心跳检测(3min离线判定)              │
└─────────────────────────────────────────────────┘
```

### 2.2 硬件切换设计

- 定义 `DataCollector` 接口：`void collect(SensorData data)`, `void start()`, `void stop()`
- `MockDataCollector`：**动态轮询机制** — 每次触发时先 `SELECT id FROM device WHERE status='ONLINE' AND device_category='SENSOR'`，遍历活跃传感器列表逐个生成模拟数据，绝不硬编码设备ID。新绑定设备无需重启即可看到数据跳动
- `MqttDataCollector`：订阅MQTT Topic（sensor/+/data），接收真实硬件数据
- 切换方式：`application.yml` 中 `data.collector: mock|mqtt`

---

## 3. 数据库设计 (8张表 — 含完整字段和约束)

### 3.1 user (用户表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| username | VARCHAR(50) | UNIQUE NOT NULL | 登录名 |
| password | VARCHAR(255) | NOT NULL | BCrypt加密 |
| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | FARMER / ADMIN / SUPER_ADMIN |
| phone | VARCHAR(20) | | 手机号 |
| enabled | TINYINT(1) | DEFAULT 1 | 启用状态 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

### 3.2 plot (地块表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| name | VARCHAR(100) | NOT NULL | 地块名称 |
| location | VARCHAR(255) | | 地理位置 |
| area | DECIMAL(10,2) | | 面积(亩) |
| crop_type | VARCHAR(50) | | 种植作物 |
| owner_id | BIGINT | FK→user.id, NOT NULL | 归属农户 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

### 3.3 device (设备表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| device_code | VARCHAR(100) | UNIQUE NOT NULL | 设备唯一编码 |
| device_name | VARCHAR(100) | NOT NULL | 设备名称 |
| device_type | VARCHAR(50) | NOT NULL | 设备类型: TEMP_HUMIDITY / SOIL_MOISTURE / IRRIGATION / LIGHT / VENTILATION |
| **device_category** | VARCHAR(20) | **NOT NULL** | **SENSOR / CONTROLLER** |
| plot_id | BIGINT | FK→plot.id, NULLABLE | 绑定地块(NULL=仓库中) |
| status | VARCHAR(20) | DEFAULT 'OFFLINE' | ONLINE / OFFLINE / FAULT |
| mqtt_topic | VARCHAR(255) | | MQTT订阅/发布Topic |
| install_location | VARCHAR(255) | | 安装位置描述(如"东区") |
| last_heartbeat | DATETIME | | 最后心跳时间 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

### 3.4 sensor_data (传感器数据表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| device_id | BIGINT | FK→device.id, NOT NULL | |
| data_type | VARCHAR(50) | NOT NULL | TEMPERATURE / HUMIDITY / SOIL_MOISTURE / LIGHT_INTENSITY / CO2 |
| data_value | DECIMAL(10,2) | NOT NULL | 采集值 |
| unit | VARCHAR(20) | | °C / % / lux / ppm |
| collect_time | DATETIME | NOT NULL | 采集时间 |
| source | VARCHAR(10) | DEFAULT 'MOCK' | MOCK / MQTT |
| create_time | DATETIME | DEFAULT NOW() | |

### 3.5 alert_rule (告警规则表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| device_id | BIGINT | FK→device.id, NULLABLE | NULL=全局规则 |
| plot_id | BIGINT | FK→plot.id, NULLABLE | |
| metric_type | VARCHAR(50) | NOT NULL | 监控指标类型 |
| rule_name | VARCHAR(100) | NOT NULL | 规则名称 |
| min_threshold | DECIMAL(10,2) | | 下限阈值 |
| max_threshold | DECIMAL(10,2) | | 上限阈值 |
| alert_level | VARCHAR(20) | NOT NULL | CRITICAL / WARNING / INFO |
| enabled | TINYINT(1) | DEFAULT 1 | |
| notify_method | VARCHAR(50) | | WEBSOCKET / SMS |
| create_time | DATETIME | DEFAULT NOW() | |

### 3.6 alert_log (告警记录表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| rule_id | BIGINT | FK→alert_rule.id | |
| device_id | BIGINT | FK→device.id | |
| plot_id | BIGINT | FK→plot.id | |
| alert_msg | VARCHAR(500) | NOT NULL | 告警内容 |
| alert_level | VARCHAR(20) | NOT NULL | |
| current_value | DECIMAL(10,2) | | 触发时的实际值 |
| is_read | TINYINT(1) | DEFAULT 0 | |
| trigger_time | DATETIME | NOT NULL | |
| create_time | DATETIME | DEFAULT NOW() | |

### 3.7 control_log (控制指令表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| device_id | BIGINT | FK→device.id, NOT NULL | |
| plot_id | BIGINT | FK→plot.id | |
| operator_id | BIGINT | FK→user.id, NOT NULL | |
| command | VARCHAR(50) | NOT NULL | ON / OFF / SET_PARAM |
| command_params | VARCHAR(255) | | JSON参数(如定时时长) |
| command_status | VARCHAR(20) | NOT NULL | PENDING / SENT / SUCCESS / FAILED / TIMEOUT |
| result_msg | TEXT | | 执行结果或失败原因 |
| send_time | DATETIME | NOT NULL | 指令发送时间 |
| response_time | DATETIME | | 设备响应时间 |
| create_time | DATETIME | DEFAULT NOW() | |

### 3.8 knowledge_base (知识库表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | |
| question | VARCHAR(500) | NOT NULL | 标准问题 |
| answer | TEXT | NOT NULL | 标准答案 |
| category | VARCHAR(50) | | IRRIGATION/FERTILIZER/PEST/DISEASE/GENERAL |
| keywords | VARCHAR(255) | | 逗号分隔关键词 |
| crop_type | VARCHAR(50) | | 关联作物 |
| create_time | DATETIME | DEFAULT NOW() | |

---

## 4. 预置数据规格 (data.sql)

### 4.1 用户初始化

| 用户名 | 密码(明文) | 角色 | 真实姓名 |
|--------|-----------|------|---------|
| admin | admin123 | ADMIN | 系统管理员 |
| farmer1 | farmer123 | FARMER | 农户老王 |
| farmer2 | farmer123 | FARMER | 农户老李 |

### 4.2 地块初始化

| 地块 | 归属 | 面积 | 作物 |
|------|------|------|------|
| 地块A · 温室大棚 | farmer1 (农户老王) | 2.5亩 | 番茄 |
| 地块B · 露天农田 | farmer2 (农户老李) | 5.0亩 | 水稻 |

### 4.3 设备初始化 — 关键场景配置

| 设备编码 | 类型 | 类别 | 绑定地块 | 状态 | 说明 |
|---------|------|------|---------|------|------|
| DEV-TH-001 | TEMP_HUMIDITY | SENSOR | 地块A | ONLINE | 温湿度传感器 |
| DEV-IR-001 | IRRIGATION | CONTROLLER | 地块A | ONLINE | 灌溉控制器(东区) |
| DEV-TH-002 | TEMP_HUMIDITY | SENSOR | 地块B | ONLINE | 温湿度传感器 |
| DEV-IR-002 | IRRIGATION | CONTROLLER | **NULL(仓库)** | OFFLINE | 未绑定控制器 |

**场景验证矩阵：**
- 地块A：有SENSOR + CONTROLLER → 灌溉按钮正常显示
- 地块B：只有SENSOR，无CONTROLLER → **空状态**："当前农田未配置控制设备，无法进行远程操作"
- 仓库中：DEV-IR-002 未绑定 → 管理员可将其绑定到任意地块

### 4.4 知识库预置 (至少5条)

| 问题 | 答案 | 分类 |
|------|------|------|
| 番茄苗期湿度多少合适？ | 番茄苗期土壤湿度应保持在60%-80%之间... | IRRIGATION |
| 水稻分蘖期需要多少水？ | 水稻分蘖期需保持3-5cm浅水层... | IRRIGATION |
| 土壤湿度过低怎么办？ | 立即开启灌溉，建议采用滴灌方式... | IRRIGATION |
| 如何判断作物是否缺水？ | 观察叶片是否萎蔫、土壤表面是否干裂... | GENERAL |
| 温室大棚温度过高如何处理？ | 开启通风设备，必要时使用遮阳网... | GENERAL |

---

## 5. API设计 (RESTful, 前缀 /api/v1)

### 5.1 认证模块

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | /auth/login | 无 | 登录，返回JWT |
| GET | /auth/me | JWT | 获取当前用户信息 |

### 5.2 地块管理 (ADMIN写 / FARMER读自己)

| 方法 | 路径 | 鉴权 | SQL强制条件 |
|------|------|------|------------|
| GET | /plot/list | FARMER+ | `WHERE owner_id={currentUserId}` (农户) / 全部(ADMIN) |
| GET | /plot/{id}/overview | FARMER+ | 校验plot.owner_id = currentUserId |
| POST | /plot | **ADMIN ONLY** | 新增地块，必须指定owner_id |
| PUT | /plot/{id} | **ADMIN ONLY** | 修改地块信息 |
| DELETE | /plot/{id} | **ADMIN ONLY** | 删除地块(需先解绑设备) |

### 5.3 设备管理

| 方法 | 路径 | 鉴权 | SQL强制条件 |
|------|------|------|------------|
| GET | /device/list?plotId= | FARMER+ | 按plotId查，校验plot归属 |
| GET | /device/unbound | **ADMIN ONLY** | 查询仓库中未绑定设备(plot_id IS NULL) |
| POST | /device/bind | **ADMIN ONLY** | 绑定设备到地块(校验设备未被占用) |
| POST | /device/unbind/{id} | **ADMIN ONLY** | 解绑设备(设置plot_id=NULL) |
| GET | /device/status/{id} | FARMER+ | 校验归属后返回设备在线状态 |

### 5.4 传感器数据

| 方法 | 路径 | 鉴权 | SQL强制条件 |
|------|------|------|------------|
| GET | /sensor/realtime/{deviceId} | FARMER+ | 校验设备归属 |
| GET | /sensor/history?deviceId=&days=7 | FARMER+ | 校验设备归属 |

### 5.5 设备控制 — 四级强校验

| 方法 | 路径 | 鉴权 | 校验链 |
|------|------|------|--------|
| POST | /control/irrigation | FARMER+ | ①归属权 ②设备类型(CONTROLLER) ③在线状态 ④写日志 |
| GET | /control/log?deviceId=&page=&size= | FARMER+ | 校验归属 |

**POST /control/irrigation 请求体：**
```json
{
  "deviceId": 2,
  "command": "ON",
  "duration": 30
}
```

**四级校验链（任一失败即抛异常，返回400）：**

| 步骤 | 校验内容 | 失败响应 |
|------|---------|---------|
| ① 归属权 | `SELECT plot_id FROM device WHERE id=?` → 查 `plot.owner_id` == currentUserId | 403 "无权操作此设备" |
| ② 设备类型 | `device_category == CONTROLLER` | 400 "传感器设备不支持控制操作" |
| ③ 在线状态 | `status == ONLINE` | 400 "设备离线，指令下发失败" |
| ④ 日志写入 | INSERT control_log (PENDING) | — |

**指令超时闭环（10秒）：**
1. 发送指令 → control_log.status = SENT, send_time = NOW()
2. 启动异步超时任务(CompletableFuture + 10s timeout)
3. 等待设备响应Topic: `device/{id}/response`
4. 收到响应 → status = SUCCESS, response_time = NOW()
5. 超时 → status = TIMEOUT, result_msg = "设备连接超时，10秒内未收到响应"
6. **TIMEOUT/FAILED均通过WebSocket推送给前端** + 写入 control_log

### 5.6 告警管理

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | /alert/rule?plotId= | FARMER+ | 查询地块告警规则 |
| POST | /alert/rule | FARMER+ | 创建/更新告警规则(校验plot归属) |
| GET | /alert/log?page=&size=&level= | FARMER+ | 告警日志分页(只查自己地块) |
| PUT | /alert/log/{id}/read | FARMER+ | 标记已读 |

### 5.7 AI智能问答

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | /ai/chat | FARMER+ | **自动注入当前地块实时环境参数** |
| GET | /ai/knowledge?category= | FARMER+ | 知识库列表 |

**POST /ai/chat 请求体：**
```json
{
  "question": "当前土壤湿度合适吗？需要浇水吗？",
  "plotId": 1
}
```

**后端处理流程：**
1. 校验plot归属
2. 查询该地块所有SENSOR设备最新 sensor_data → 组装环境上下文
3. RAG检索 knowledge_base 匹配相关答案
4. 组装Prompt: `[环境参数: 温度26.5°C, 湿度68%...] + [知识库参考] + 用户问题`
5. 调用LLM/规则引擎生成回答

---

## 6. WebSocket 实时推送

| Topic | 推送内容 | 触发时机 |
|-------|---------|---------|
| `/topic/plot/{plotId}/sensors` | 传感器实时数据JSON | Mock/MQTT每次采集 |
| `/topic/user/{userId}/alerts` | 新告警通知 | AlertEngine触发 |
| `/topic/device/{id}/status` | 设备在线状态变更 + 控制指令结果 | 心跳超时/指令反馈 |

---

## 7. 前端业务规约

### 7.1 PC端 (Vue 3 + Element Plus + ECharts)

**必须实现的动态降级逻辑：**

```vue
<!-- 控制面板区域 -->
<template v-if="controllers.length > 0">
  <!-- 渲染灌溉开关、通风控制等 -->
</template>
<template v-else>
  <el-empty description="当前农田未配置控制设备，无法进行远程操作" 
            :image-size="120" />
</template>

<!-- 设备卡片列表 -->
<template v-if="devices.length > 0">
  <DeviceCard v-for="d in devices" :key="d.id" :device="d" />
</template>
<template v-else>
  <el-empty description="该地块暂无绑定设备" />
</template>
```

**设备类型判断必须使用：** `device.deviceCategory === 'CONTROLLER'` (前端camelCase)

### 7.2 移动端 (uni-app)

**同样的空状态逻辑，使用uni-app语法：**
```vue
<view v-if="controllers.length > 0">
  <!-- 控制面板 -->
</view>
<view v-else class="empty-state">
  <image src="/static/empty-device.png" />
  <text>当前农田未配置控制设备，无法进行远程操作</text>
</view>
```

### 7.3 指令超时前端处理

```javascript
// 发送控制指令后
const sendCommand = async (deviceId, command) => {
  try {
    const res = await api.controlIrrigation({ deviceId, command })
    // WebSocket 监听响应
    const timeout = setTimeout(() => {
      ElMessage.warning('设备连接超时，请检查设备状态')
    }, 10000)
    // 收到WebSocket响应后 clearTimeout(timeout)
  } catch (e) {
    ElMessage.error(e.message) // "设备离线，指令下发失败"
  }
}
```

---

## 8. AI智能体设计

### 8.1 上下文注入流程

```
用户提问 "需要浇水吗？" + plotId=1
  → 查plot归属
  → SELECT * FROM sensor_data WHERE device_id IN 
    (SELECT id FROM device WHERE plot_id=1 AND device_category='SENSOR')
    ORDER BY collect_time DESC LIMIT 10
  → 组装环境上下文: "当前地块A环境：温度26.5°C，土壤湿度68%，光照850lux..."
  → RAG检索: SELECT * FROM knowledge_base WHERE keywords LIKE '%浇水%湿度%'
  → 最终Prompt: "你是农业专家。当前环境参数: {...}。参考知识库: {...}。用户问题: 需要浇水吗？请给出基于当前数据的诊断建议。"
  → LLM/规则引擎 → 返回针对性回答
```

### 8.2 三种工作模式

| 模式 | 实现 | 场景 |
|------|------|------|
| 规则引擎 | MySQL全文索引 + 阈值判断 + 模板回答 | 校内演示、无网络 |
| 大模型 | DeepSeek API + RAG上下文(含实时数据) | 高质量回答 |
| 混合(默认) | 规则优先，无法匹配降级LLM | 推荐 |

---

## 9. 告警引擎与心跳检测

### 9.1 告警规则

| 规则 | 条件 | 级别 | 通知 |
|------|------|------|------|
| 土壤湿度过低 | humidity < min_threshold | 🔴 CRITICAL | WebSocket实时推送 |
| 土壤湿度预警 | humidity < min_threshold * 1.2 | 🟡 INFO | 仅写入日志 |
| 温度过高 | temperature > 40°C | 🔴 CRITICAL | WebSocket实时推送 |
| 设备离线 | 3分钟无心跳 | 🟠 WARNING | WebSocket + 设备卡片置灰 |

### 9.2 心跳检测机制

```
@Scheduled(fixedRate = 60000)  // 每分钟执行一次
public void checkDeviceHeartbeat() {
    List<Device> onlineDevices = deviceMapper.selectList(
        Wrappers.<Device>lambdaQuery().eq(Device::getStatus, "ONLINE")
    );
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(3);
    for (Device device : onlineDevices) {
        if (device.getLastHeartbeat().isBefore(threshold)) {
            device.setStatus("OFFLINE");
            deviceMapper.updateById(device);
            // WebSocket 推送设备离线通知
            wsService.sendDeviceStatusChange(device.getId(), "OFFLINE");
            // 写入告警日志
            alertLogService.log(device, "设备离线", WARNING);
        }
    }
}
```

---

## 10. MockDataCollector 动态轮询规约

```java
@Scheduled(fixedRateString = "${data.mock.interval:5000}")
public void generateMockData() {
    // 第一步：动态查询所有在线的SENSOR设备
    List<Device> activeSensors = deviceMapper.selectList(
        Wrappers.<Device>lambdaQuery()
            .eq(Device::getStatus, "ONLINE")
            .eq(Device::getDeviceCategory, "SENSOR")
    );
    // 第二步：遍历生成模拟数据
    for (Device sensor : activeSensors) {
        SensorData data = generateRandomData(sensor);
        sensorDataMapper.insert(data);
        // 第三步：通过WebSocket推送给对应地块的所有订阅者
        wsService.sendSensorData(sensor.getPlotId(), data);
    }
    // 第四步：触发告警引擎扫描
    alertEngine.scan(activeSensors);
}
```

**绝不硬编码设备ID**。管理员通过API绑定新设备后，下一次定时任务自动发现并开始为其生成数据。

---

## 11. 工程结构 (Maven多模块)

```
smart-agriculture/
├── pom.xml                              # 父POM
├── smart-agriculture-common/            # 公共模块
│   └── com/smartfarm/common/
│       ├── R.java                       # 统一响应 {code, message, data}
│       ├── exception/BizException.java  # 业务异常
│       ├── exception/GlobalExceptionHandler.java
│       └── config/
├── smart-agriculture-framework/         # 框架模块
│   └── com/smartfarm/framework/
│       ├── security/JwtAuthFilter.java  # JWT认证拦截器
│       ├── security/UserContext.java    # 当前用户上下文
│       ├── websocket/WebSocketConfig.java
│       └── websocket/WsPushService.java
├── smart-agriculture-business/          # 业务模块
│   └── com/smartfarm/business/
│       ├── monitor/   (SensorController, SensorService, SensorDataMapper)
│       ├── device/    (DeviceController, DeviceService, DeviceMapper)
│       ├── control/   (ControlController, ControlService, CommandValidator)
│       ├── alert/     (AlertController, AlertEngine, AlertMapper)
│       ├── ai/        (AiController, RagService, LlmService, KnowledgeMapper)
│       ├── plot/      (PlotController, PlotService, PlotMapper)
│       └── auth/      (AuthController, AuthService, UserMapper)
├── smart-agriculture-simulator/         # 模拟数据模块(可插拔)
│   └── com/smartfarm/simulator/
│       ├── DataCollector.java           # 数据采集接口
│       ├── MockDataCollector.java       # 动态轮询实现
│       └── MockDataGenerator.java      # 合理模拟数据生成器
├── smart-agriculture-web/               # PC前端 Vue 3
├── smart-agriculture-mobile/            # 移动端 uni-app
└── smart-agriculture-server/            # 启动模块
    └── src/main/resources/
        ├── application.yml
        ├── application-mock.yml
        ├── application-prod.yml
        └── db/
            ├── schema.sql               # 建表脚本
            └── data.sql                 # 预置数据
```

---

## 12. 技术版本清单

| 技术 | 版本 |
|------|------|
| JDK | 21 (OpenJDK) |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.6 |
| MySQL | 8.0 |
| Lombok | 1.18.32 |
| jjwt | 0.12.5 |
| Spring WebSocket | 内嵌 |
| Maven | 3.9+ (使用Wrapper) |
| Vue | 3.4+ |
| Element Plus | 2.7+ |
| ECharts | 5.5+ |
| uni-app | 最新稳定版 |
| Node.js | 18+ |

---

## 13. 验收标准自审

- [x] schema.sql 包含所有表及外键关系
- [x] data.sql 包含预置管理员、2农户、2地块、4设备(含SENSOR和CONTROLLER分离)
- [x] 地块B无CONTROLLER，验证空状态展示
- [x] 1台CONTROLLER在仓库中(plot_id=NULL)
- [x] device表含device_category字段(SENSOR/CONTROLLER)
- [x] 所有查询SQL附加owner_id水平越权拦截
- [x] 控制指令四级校验链(归属/类型/在线/日志)
- [x] 指令超时10秒闭环机制
- [x] 心跳检测3分钟离线判定
- [x] MockDataCollector动态轮询不硬编码设备ID
- [x] AI问答注入实时环境参数
- [x] 前端v-if/v-else空状态降级逻辑
- [x] 所有接口使用R.java统一响应封装
- [x] MyBatis-Plus分页插件配置
- [x] Lombok实体类
