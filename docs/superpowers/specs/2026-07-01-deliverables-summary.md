# 智慧农业实训项目 — 架构交付物总结

> **角色关系**：本文档由**架构师/执行开发者（Claude）**输出，供**分析师（另一AI）**审核校验。  
> 日期：2026-07-01 | 阶段：前15天校内阶段  
> **环境**：JDK 21 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.6 + MySQL 8.0 (root/123456) + Vue 3 + uni-app

---

## 一、已完成工作总览

### 1.1 设计规格说明书 (v2.0)
**文件**：`docs/superpowers/specs/2026-07-01-smart-agriculture-design.md`

包含：
- 系统架构（三层 + 双端 + DataCollector接口抽象实现Mock/真实硬件切换）
- 8张数据表完整字段定义
- 20+ REST API接口规范（含鉴权级别和SQL强制条件）
- WebSocket 3个Topic推送规范
- PC端10个路由页面 + 移动端4个Tab页面设计
- AI智能体RAG流程 + 双模式（规则/LLM）切换
- 告警引擎规则 + 3分钟心跳检测机制
- MockDataCollector动态轮询机制（不硬编码设备ID）
- 完整ACID验收自审清单

### 1.2 已编码文件清单 (28个文件)

#### 数据库脚本 (2个)
| 文件 | 路径 | 说明 |
|------|------|------|
| schema.sql | `smart-agriculture-server/src/main/resources/db/schema.sql` | 8张表完整DDL，含FOREIGN KEY、INDEX、FULLTEXT |
| data.sql | `smart-agriculture-server/src/main/resources/db/data.sql` | 预置数据（见下文场景矩阵） |

#### 公共模块 smart-agriculture-common (3个)
| 文件 | 路径 | 说明 |
|------|------|------|
| R.java | `common/.../R.java` | 统一响应封装 `{code, message, data}`，含ok/fail/forbidden/badRequest工厂方法 |
| BizException.java | `common/.../exception/BizException.java` | 业务异常基类，6种预定义工厂（deviceOffline/sensorNotControllable/noPermission/deviceAlreadyBound/commandTimeout/plotHasDevices） |
| GlobalExceptionHandler.java | `common/.../exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` 全局异常拦截，BizException→400/403，未知异常→500 |

#### 实体类 smart-agriculture-business/entity (8个)
| 实体 | 关键字段 | 核心设计 |
|------|---------|---------|
| User.java | id, username, password(BCrypt), role(FARMER/ADMIN/SUPER_ADMIN) | role枚举，enabled软开关 |
| Plot.java | id, name, area, crop_type, **owner_id**(FK→user) | RBAC隔离基础 |
| **Device.java** | id, device_code, device_type, **device_category(SENSOR/CONTROLLER)**, plot_id(NULL=仓库), status, last_heartbeat | **核心实体**，category区分职能 |
| SensorData.java | device_id, data_type, data_value, unit, collect_time, source(MOCK/MQTT) | source字段支持模拟/真实溯源 |
| AlertRule.java | device_id, metric_type, min_threshold, max_threshold, alert_level | 动态阈值配置 |
| AlertLog.java | rule_id, device_id, plot_id, alert_msg, alert_level, current_value, is_read | 审计追踪 |
| ControlLog.java | device_id, operator_id, command, **command_status(PENDING→SENT→SUCCESS/FAILED/TIMEOUT)**, result_msg, send_time, response_time | **状态机流转** |
| KnowledgeBase.java | question, answer, category, keywords, crop_type | RAG检索数据源 |

#### Mapper接口 (8个)
全部位于 `smart-agriculture-business/.../mapper/`，继承 `BaseMapper<T>`：
UserMapper, PlotMapper, DeviceMapper, SensorDataMapper, ControlLogMapper, AlertRuleMapper, AlertLogMapper, KnowledgeBaseMapper

#### 框架模块 smart-agriculture-framework (2个)
| 文件 | 说明 |
|------|------|
| security/UserContext.java | ThreadLocal存储当前用户(LoginUser Record含id/username/role/realName)，静态方法getCurrentUserId() |
| websocket/WsPushService.java | 统一推送：sendSensorData(plotId)、sendAlert(plotId)、sendDeviceStatusChange(deviceId)、sendCommandResult(deviceId) → 3个Topic |

#### 核心业务服务 (2个)
| 文件 | 职责 |
|------|------|
| business/control/ControlService.java | **四级校验链 + 10秒超时闭环**（见第四节详解） |
| business/alert/AlertEngine.java | 动态加载alert_rule规则 → 对比传感器最新值 → 超阈值触发告警 → CRITICAL级别WebSocket推送 |

#### 模拟数据层 smart-agriculture-simulator (3个)
| 文件 | 核心技术点 |
|------|-----------|
| DataCollector.java | 接口：start()/stop()/getName()，Mock和MQTT两套实现 |
| MockDataCollector.java | **@Scheduled动态轮询**：每次先`SELECT id FROM device WHERE status='ONLINE' AND device_category='SENSOR'`，再遍历生成。连续平滑变化(±0.5°C温度/±2%湿度)，不硬编码设备ID |
| HeartbeatChecker.java | **@Scheduled每60秒**：扫描ONLINE设备，last_heartbeat超过3分钟→自动OFFLINE→WebSocket推送+写告警日志 |

---

## 二、环境配置信息

| 配置项 | 值 |
|--------|-----|
| JDK版本 | 21 (OpenJDK, 已安装) |
| 构建工具 | Maven 3.9+ (待安装Maven Wrapper) |
| IDE | IntelliJ IDEA (已配置JDK 21) |
| 数据库 | MySQL 8.0 |
| 数据库账号 | **root** |
| 数据库密码 | **123456** |
| 数据库名 | smart_agriculture |
| 前端风格约束 | **禁止使用emoji图案**，使用Element Plus / uni-app内置图标；缺少图标时从开源项目(如iconfont、Material Icons)引入 |

---

## 三、预置数据场景矩阵 (data.sql)

### 用户
| ID | 用户名 | 密码(BCrypt) | 角色 |
|----|--------|-------------|------|
| 1 | admin | admin123 | ADMIN |
| 2 | farmer1 | farmer123 | FARMER (农户老王) |
| 3 | farmer2 | farmer123 | FARMER (农户老李) |

### 地块
| ID | 名称 | 面积 | 作物 | 归属 |
|----|------|------|------|------|
| 1 | 地块A · 温室大棚 | 2.5亩 | 番茄 | farmer1(老王) |
| 2 | 地块B · 露天农田 | 5.0亩 | 水稻 | farmer2(老李) |

### 设备 — 关键场景配置
| ID | 编码 | 类型 | 类别 | 绑定 | 状态 | 场景验证 |
|----|------|------|------|------|------|---------|
| 1 | DEV-TH-001 | TEMP_HUMIDITY | **SENSOR** | 地块A | ONLINE | 正常数据采集 |
| 2 | DEV-IR-001 | IRRIGATION | **CONTROLLER** | 地块A | ONLINE | **灌溉按钮应正常显示** |
| 3 | DEV-TH-002 | TEMP_HUMIDITY | **SENSOR** | 地块B | ONLINE | 正常数据采集 |
| 4 | DEV-IR-002 | IRRIGATION | **CONTROLLER** | **NULL(仓库)** | OFFLINE | 管理员可绑定到任意地块 |

### 场景验证清单
| 场景 | 预期行为 |
|------|---------|
| 农户老王查看地块A | 看到SENSOR数据 + CONTROLLER灌溉按钮（正常操作） |
| 农户老李查看地块B | 看到SENSOR数据 + **空状态提示**："当前农田未配置控制设备，无法进行远程操作" |
| 管理员查看仓库设备 | DEV-IR-002 plot_id=NULL，可绑定到地块A或B |
| 农户老王尝试查看地块B | **403越权拦截**，SQL自动附加WHERE owner_id=2 |
| 新设备绑定后 | MockDataCollector下一次定时任务自动发现，无需重启 |

---

## 四、控制指令四级校验 + 超时闭环机制

### 4.1 调用链路
```
POST /api/v1/control/irrigation {deviceId: 2, command: "ON", duration: 30}
  → ControlService.executeCommand(deviceId, command, duration)
```

### 4.2 四级校验链

| 步骤 | 校验内容 | SQL/逻辑 | 失败异常 | HTTP |
|------|---------|---------|---------|------|
| ①归属权 | 设备归属的plot.owner_id == currentUserId | `SELECT plot.owner_id FROM device JOIN plot ON device.plot_id=plot.id WHERE device.id=?` → 对比UserContext.getCurrentUserId() | BizException.noPermission() | 403 |
| ②设备类型 | device_category MUST BE 'CONTROLLER' | `device.getDeviceCategory().equals("CONTROLLER")` | BizException.sensorNotControllable() | 400 |
| ③在线状态 | status MUST BE 'ONLINE' | `device.getStatus().equals("ONLINE")` | BizException.deviceOffline() **且离线也写入control_log** | 400 |
| ④日志写入 | INSERT control_log (PENDING→SENT) | `controlLogMapper.insert(log)` | — | — |

### 4.3 超时闭环时序图

```
前端 sendCommand()
  │
  ▼
ControlService.executeCommand()
  │
  ├─ 四级校验通过
  ├─ control_log.status = SENT, send_time = NOW()
  ├─ publishMqttCommand(device, cmd)
  │
  ├─ CompletableFuture<String> responseFuture
  ├─ responseFutures.put(logId, future)
  │
  ├─ future.get(10, TimeUnit.SECONDS)  ← 阻塞10秒
  │   │
  │   ├─ [设备响应到达]
  │   │   → onDeviceResponse(logId, response) → future.complete(response)
  │   │   → status = SUCCESS, response_time = NOW()
  │   │   → wsPushService.sendCommandResult() → 前端显示✅成功
  │   │
  │   └─ [10秒超时]
  │       → TimeoutException
  │       → status = TIMEOUT, result_msg = "设备连接超时，10秒内未收到响应"
  │       → wsPushService.sendCommandResult() → 前端Toast "设备连接超时"
  │       → throw BizException.commandTimeout()
  │
  └─ finally: responseFutures.remove(logId)
```

### 4.4 关键数据结构
```java
// 设备响应等待池
private final Map<Long, CompletableFuture<String>> responseFutures 
    = new ConcurrentHashMap<>();

// MQTT监听器回调入口
public void onDeviceResponse(Long controlLogId, String response) {
    CompletableFuture<String> future = responseFutures.get(controlLogId);
    if (future != null && !future.isDone()) {
        future.complete(response);
    }
}
```

### 4.5 Mock模式模拟响应
```java
// 校内Mock模式: 自动模拟2秒后设备响应
CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
    // 查询最新SENT状态的control_log
    // → 调用 onDeviceResponse() 完成闭环
});
// 生产环境删除此段代码，改为真实MQTT回调
```

---

## 五、前端UI交互规约 (待实现)

### 5.1 图标规范
- **禁止使用emoji**（如🌾🌡️💧等）
- PC端：使用 **Element Plus Icon** 组件（`<el-icon>`）
- 移动端：使用 **uni-app内置图标** 或 **uni-icons插件**
- 缺少的图标：从以下来源引入
  - [iconfont](https://www.iconfont.cn/) — 阿里巴巴矢量图标库，搜索"农业""传感器""灌溉"等关键词
  - [Material Design Icons](https://pictogrammers.com/library/mdi/) — 开源图标集
  - [Element Plus Icons](https://element-plus.org/en-US/component/icon.html) — 官方图标集

### 5.2 动态降级逻辑 (必须实现)

**PC端 Vue 3：**
```vue
<!-- 控制面板：根据设备列表动态渲染 -->
<template v-if="controllers.length > 0">
  <el-row :gutter="16">
    <el-col v-for="ctrl in controllers" :key="ctrl.id" :span="8">
      <ControlCard :device="ctrl" @command="handleCommand" />
    </el-col>
  </el-row>
</template>
<template v-else>
  <el-empty description="当前农田未配置控制设备，无法进行远程操作" :image-size="120" />
</template>

<!-- 设备卡片列表空状态 -->
<template v-if="devices.length === 0">
  <el-empty description="该地块暂无绑定设备" />
</template>
```

**移动端 uni-app：**
```vue
<view v-if="controllers.length > 0">
  <view v-for="ctrl in controllers" :key="ctrl.id">
    <ControlCard :device="ctrl" />
  </view>
</view>
<view v-else class="empty-state">
  <image src="/static/icons/empty-device.png" mode="aspectFit" />
  <text class="empty-text">当前农田未配置控制设备，无法进行远程操作</text>
</view>
```

### 5.3 指令超时前端处理
```javascript
const sendCommand = async (deviceId, command) => {
  try {
    // 同时监听WebSocket响应
    const wsHandler = (msg) => {
      if (msg.deviceId === deviceId) {
        if (msg.status === 'SUCCESS') {
          ElMessage.success('指令执行成功')
        } else if (msg.status === 'TIMEOUT') {
          ElMessage.warning('设备连接超时，请检查设备状态')
        } else if (msg.status === 'FAILED') {
          ElMessage.error(msg.message)
        }
        clearTimeout(wsTimeout)
      }
    }
    const wsTimeout = setTimeout(() => {
      ElMessage.warning('设备连接超时，请检查设备状态')
    }, 10000)

    await api.controlIrrigation({ deviceId, command })
  } catch (e) {
    ElMessage.error(e.message)
  }
}
```

---

## 六、待实现工作清单

### 6.1 后端 (优先级排列)
- [ ] Spring Boot主应用类 + 启动配置 (application.yml)
- [ ] Maven父POM + 各子模块POM (含依赖管理)
- [ ] Maven Wrapper配置
- [ ] JWT认证Filter + Spring Security配置
- [ ] MyBatis-Plus分页插件配置
- [ ] Controller层全部实现 (Auth/Plot/Device/Sensor/Control/Alert/AI)
- [ ] Service层完整实现 (PlotService含RBAC过滤/DeviceService含绑定校验)
- [ ] AI模块 (RagService + LlmService)
- [ ] 数据库自动初始化 (schema.sql + data.sql 启动时执行)
- [ ] WebSocket STOMP配置

### 6.2 前端 PC端 (Vue 3 + Element Plus)
- [ ] 项目脚手架搭建 (Vite + Vue Router + Axios + Element Plus + ECharts)
- [ ] 登录页面 + JWT Token管理
- [ ] 数据驾驶舱主页 (Dashboard Hub)
- [ ] 地块实时监测页面 + ECharts趋势图
- [ ] 设备管理页面 (列表 + 绑定/解绑)
- [ ] 设备控制页面 (含空状态组件)
- [ ] 告警中心页面 (列表 + 规则配置表单)
- [ ] AI农事助手页面 (对话界面)
- [ ] 全屏数据大屏页面
- [ ] WebSocket客户端封装

### 6.3 前端 移动端 (uni-app)
- [ ] 项目脚手架搭建
- [ ] 登录页面
- [ ] 首页(监测Tab) + 农田选择器 + 设备卡片
- [ ] 控制Tab + 设备选择 + 操作面板
- [ ] AI Tab + 聊天界面
- [ ] 我的Tab + 告警列表 + 设置
- [ ] WebSocket + API封装

---

## 七、文件目录树

```
smart-agriculture/
├── docs/superpowers/specs/
│   ├── 2026-07-01-smart-agriculture-design.md   ← 设计规格说明书 v2.0
│   └── 2026-07-01-deliverables-summary.md       ← 本文档
│
├── smart-agriculture-common/src/main/java/com/smartfarm/common/
│   ├── R.java
│   └── exception/
│       ├── BizException.java
│       └── GlobalExceptionHandler.java
│
├── smart-agriculture-framework/src/main/java/com/smartfarm/framework/
│   ├── security/
│   │   └── UserContext.java
│   └── websocket/
│       └── WsPushService.java
│
├── smart-agriculture-business/src/main/java/com/smartfarm/business/
│   ├── entity/
│   │   ├── User.java
│   │   ├── Plot.java
│   │   ├── Device.java                        ← 含 device_category 字段
│   │   ├── SensorData.java
│   │   ├── AlertRule.java
│   │   ├── AlertLog.java
│   │   ├── ControlLog.java                    ← 含状态机 PENDING→SENT→SUCCESS/FAILED/TIMEOUT
│   │   └── KnowledgeBase.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── PlotMapper.java
│   │   ├── DeviceMapper.java
│   │   ├── SensorDataMapper.java
│   │   ├── ControlLogMapper.java
│   │   ├── AlertRuleMapper.java
│   │   ├── AlertLogMapper.java
│   │   └── KnowledgeBaseMapper.java
│   ├── control/
│   │   └── ControlService.java                ← 四级校验 + 10秒超时闭环
│   └── alert/
│       └── AlertEngine.java
│
├── smart-agriculture-simulator/src/main/java/com/smartfarm/simulator/
│   ├── DataCollector.java                     ← 接口(模拟/MQTT切换)
│   ├── MockDataCollector.java                 ← 动态轮询 @Scheduled
│   └── HeartbeatChecker.java                  ← 3分钟心跳检测
│
└── smart-agriculture-server/src/main/resources/db/
    ├── schema.sql                              ← 8张表DDL
    └── data.sql                                ← 预置数据
```

---

## 八、审核要点 (供分析师检查)

请另一AI分析师重点审核以下内容：

1. **RBAC完整性**：所有查询SQL是否强制附加 `WHERE owner_id = currentUserId`？（参考设计文档第5节API表格中的"SQL强制条件"列）
2. **设备分类**：`device_category` 字段是否在所有关键代码路径中正确使用？（ControlService校验②、前端v-if判断）
3. **空状态处理**：地块B只有SENSOR无CONTROLLER → 前端必须渲染空状态组件
4. **超时闭环**：ControlService的CompletableFuture超时机制是否正确？responseFutures是否在finally中清理？
5. **动态轮询**：MockDataCollector是否每次从数据库查询活跃设备，而非硬编码ID列表？
6. **心跳检测**：HeartbeatChecker的3分钟阈值 + WebSocket通知 + 告警日志写入是否完整？
7. **审计日志**：控制指令无论成功/失败/超时都写入control_log表了吗？
8. **数据初始化**：data.sql中的场景矩阵是否正确覆盖了所有验证场景？
9. **API鉴权**：ADMIN ONLY和FARMER+的访问控制粒度是否正确？
10. **图标规范**：前端禁止emoji，使用Element Plus Icons / uni-icons / iconfont / Material Icons
