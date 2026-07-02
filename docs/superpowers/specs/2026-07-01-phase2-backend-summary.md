# 智慧农业实训项目 — Phase 2: 后端全量实现 完成总结

> **流程**：架构师(Claude)实现 → 本文档 → 分析师审核 → 反馈修改 → 进入 Phase 3  
> 日期：2026-07-01  
> 审核状态：⏳ 待分析师审核

---

## Phase 2 完成内容

### 一、Maven 工程搭建（6个POM）

| POM | 模块 | 职责 |
|-----|------|------|
| pom.xml | 父POM | 版本统一管理（Spring Boot 3.2.5 / MyBatis-Plus 3.5.6 / MySQL 8.0.33 / jjwt 0.12.5），5个子模块聚合 |
| smart-agriculture-common/pom.xml | 公共模块 | R.java、BizException、WsEvent DTO |
| smart-agriculture-framework/pom.xml | 框架模块 | JWT、WebSocket、Security、RBAC、定时任务配置 |
| smart-agriculture-business/pom.xml | 业务模块 | 实体、Mapper、Service、Controller |
| smart-agriculture-simulator/pom.xml | 模拟模块 | DataCollector、MockDataCollector、HeartbeatChecker |
| smart-agriculture-server/pom.xml | 启动模块 | Spring Boot主类、配置文件、SQL脚本 |

**依赖方向**：common ← framework ← business ← simulator ← server（无循环依赖）

### 二、启动配置与JWT认证（7个文件）

| 文件 | 说明 |
|------|------|
| SmartFarmApplication.java | Spring Boot 启动类 |
| application.yml | 主配置：MySQL(root/123456)、MyBatis-Plus、JWT、数据库自动初始化 |
| application-mock.yml | Mock环境配置：data.collector=mock, 间隔5秒 |
| JwtUtils.java | Token生成(HS256) + 校验 + 解析(userId/username/role) |
| JwtAuthFilter.java | OncePerRequestFilter → 解析Bearer Token → 注入UserContext + SecurityContext |
| SecurityConfig.java | Spring Security：CSRF禁用、无状态会话、ADMIN/FARMER权限路由、CORS |
| UserContext.java | ThreadLocal存储当前用户LoginUser |

### 三、Controller层（7个Controller，17+接口）

| Controller | 接口 | 鉴权 |
|-----------|------|------|
| AuthController | POST /auth/login, GET /auth/me | 无/JWT |
| PlotController | GET /plot/list, GET /plot/{id}/overview, POST/PUT/DELETE /plot | FARMER+/ADMIN ONLY |
| DeviceController | GET /device/list, GET /device/unbound, POST /device/bind, POST /device/unbind/{id}, GET /device/status/{id} | FARMER+/ADMIN ONLY |
| SensorController | GET /sensor/realtime/{deviceId}, GET /sensor/history（含降采样） | FARMER+ |
| ControlController | POST /control/irrigation, GET /control/log | FARMER+ |
| AlertController | GET /alert/rule, POST /alert/rule, GET /alert/log, PUT /alert/log/{id}/read | FARMER+ |
| AiController | POST /ai/chat, GET /ai/knowledge | FARMER+ |

### 四、Service层（7个Service）

| Service | 核心逻辑 |
|---------|---------|
| ControlService | 四级校验链 + 10秒CompletableFuture超时闭环 |
| ControlLogService | REQUIRES_NEW 独立事务审计（永不被回滚） |
| PlotService | RBAC动态过滤（ADMIN全局/FARMER行级）+ CRUD |
| DeviceService | 绑定/解绑校验（设备占用检查）+ 仓库管理 |
| SensorService | 4档TimeGranularity降采样（上限500点） |
| AlertEngine | 动态规则加载 + 上下限阈值扫描 + WebSocket推送 |
| RagService | RAG检索 + 实时环境参数注入 + 规则引擎回答 |

### 五、WebSocket与事件体系（3个文件）

| 文件 | 说明 |
|------|------|
| WebSocketConfig.java | STOMP配置：端点/ws，Broker前缀/topic，SockJS兼容 |
| WsPushService.java | 统一推送：sendSensorData / sendAlert / sendDeviceStatusChange / sendCommandResult |
| WsEvent.java | 4种推送事件DTO（SensorDataEvent/AlertEvent/DeviceStatusEvent/CommandResultEvent），位于common模块 |

---

## Phase 2 新增文件清单（22个新增 + 5个修改）

### 新增文件
```
pom.xml (父POM)
smart-agriculture-common/pom.xml
smart-agriculture-framework/pom.xml
smart-agriculture-business/pom.xml
smart-agriculture-simulator/pom.xml
smart-agriculture-server/pom.xml

SmartFarmApplication.java
application.yml
application-mock.yml

JwtUtils.java
JwtAuthFilter.java
SecurityConfig.java
WebSocketConfig.java
WsEvent.java

AuthController.java
PlotController.java
DeviceController.java
DeviceService.java
SensorController.java
ControlController.java
AlertController.java
AiController.java
RagService.java
```

### 修改文件（适配 WsPushService 签名变更）
```
WsPushService.java    → 移除business实体依赖，改用common/WsEvent DTO
ControlService.java   → 适配新的WsPushService方法签名
AlertEngine.java      → 适配
HeartbeatChecker.java → 适配
MockDataCollector.java → 适配
```

---

## 项目总览（Phase 0-2 累计）

| 模块 | 文件数 |
|------|--------|
| common | 5 |
| framework | 8 |
| business | 23 |
| simulator | 4 |
| server | 5 |
| 文档 | 5 |
| **总计** | **50个代码文件 + 5个文档** |

---

## Phase 3 展望：PC前端 (Vue 3 + Element Plus + ECharts)

Phase 3 将完成：
1. Vite项目脚手架搭建（Vue 3.4 + Vue Router + Axios + Element Plus 2.7 + ECharts 5.5）
2. 登录页面
3. Dashboard主页（Edge Fluent风格Hub）
4. 地块实时监测页面
5. 历史趋势分析页面
6. 设备管理页面 + 绑定/解绑
7. 设备控制页面（含空状态组件）
8. 告警中心页面
9. AI农事助手页面
10. 全屏数据大屏
11. WebSocket客户端封装

---

## 审核清单（供分析师检查）

1. **Maven依赖**：是否有循环依赖？common ← framework ← business ← simulator ← server 链是否正确？
2. **JWT安全**：Token过期时间24小时是否合理？密钥是否Base64编码？
3. **Security配置**：ADMIN ONLY路由是否正确？/ws/** 是否放行？CSRF是否禁用？
4. **Controller鉴权**：PlotController的增删改是否仅ADMIN？查询是否经过RBAC？
5. **数据库初始化**：schema.sql + data.sql 是否启动时自动执行？spring.sql.init.mode=always？
6. **配置文件**：数据库密码是否为123456？JWT密钥是否合适？
7. **RagService**：是否注入了实时环境参数？是否有规则引擎降级？
8. **WebSocket**：端点和Topic是否符合设计文档？前端如何订阅？
9. **WsEvent DTO**：是否位于common模块（解决循环依赖）？
10. **所有调用方是否适配新的WsPushService签名**？
