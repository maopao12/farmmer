# 智慧农业实训项目 — Phase 1: 基础设施与核心架构 完成总结

> **流程**：架构师(Claude)实现 → 本文档 → 分析师审核 → 反馈修改 → 进入 Phase 2  
> 日期：2026-07-01  
> 审核状态：⏳ 待分析师审核

---

## 项目阶段总览

| Phase | 名称 | 状态 | 产出物 |
|-------|------|------|--------|
| 0 | 需求分析与系统设计 | ✅ 完成 | 设计规格说明书 v2.0 |
| **1** | **基础设施与核心架构** | ✅ 完成 | **本文档** |
| 2 | 后端全量实现 | ⏳ 待开始 | Controller + Auth + AI模块 + 启动配置 |
| 3 | PC前端 (Vue 3) | ⏳ 待开始 | 10个路由页面 + Dashboard |
| 4 | 移动端 (uni-app) | ⏳ 待开始 | 4个Tab + 8个子页面 |
| 5 | 集成测试与交付 | ⏳ 待开始 | 全链路联调 + 验收文档 |

---

## Phase 1 完成内容

### 一、修复了分析师指出的四项致命隐患

| # | 隐患 | 严重性 | 修复方案 | 涉及文件 |
|---|------|--------|---------|---------|
| 1 | `@Transactional` 回滚导致审计日志丢失 | 🔴 致命 | 抽取 `ControlLogService`，所有日志操作使用 `REQUIRES_NEW` 独立事务。即使超时异常，SENT记录已持久化 | ControlLogService.java(新) ControlService.java(重写) |
| 2 | 一刀切 `WHERE owner_id` 导致ADMIN查不到数据 | 🔴 致命 | `PlotService.applyOwnerFilter()` 先判断角色：ADMIN跳过，FARMER才附加 `owner_id = ?` | PlotService.java(新) RbacUtils.java(新) |
| 3 | 历史查询全量拉取导致OOM | 🔴 致命 | `SensorService` 按时间跨度自动切换聚合粒度，上限500点 | SensorService.java(新) |
| 4 | 默认单线程 `@Scheduled` + for循环单条insert | 🔴 致命 | `SchedulingConfig(poolSize=10)` + `MockDataCollector` 改用 `saveBatch` 批量插入 | SchedulingConfig.java(新) MockDataCollector.java(重写) |

### 二、已创建文件清单 (37个)

#### 数据库 (2)
| 文件 | 位置 |
|------|------|
| schema.sql | smart-agriculture-server/.../db/schema.sql |
| data.sql | smart-agriculture-server/.../db/data.sql |

#### 公共模块 smart-agriculture-common (3)
| 文件 | 类名 | 职责 |
|------|------|------|
| R.java | R\<T\> | 统一响应 {code, message, data} |
| BizException.java | BizException | 业务异常 + 6种预定义工厂 |
| GlobalExceptionHandler.java | GlobalExceptionHandler | @RestControllerAdvice 全局拦截 |

#### 实体类 smart-agriculture-business/entity (8)
User, Plot, **Device**(含device_category), SensorData, AlertRule, AlertLog, **ControlLog**(含状态机), KnowledgeBase

#### Mapper接口 smart-agriculture-business/mapper (8)
全部继承 `BaseMapper<T>`，支持 MyBatis-Plus 分页和 Lambda 查询

#### 框架模块 smart-agriculture-framework (5)
| 类名 | 职责 |
|------|------|
| UserContext | ThreadLocal 当前用户上下文 |
| WsPushService | WebSocket 统一推送(3个Topic) |
| **SchedulingConfig** | TaskScheduler poolSize=10 |
| **MyBatisPlusConfig** | 分页插件 + maxLimit=500 |
| **RbacUtils** | 统一RBAC(ADMIN跳过/FARMER过滤) |

#### 业务服务 smart-agriculture-business (5)
| 类名 | 核心逻辑 |
|------|---------|
| **ControlService** | 四级校验链 + 10秒超时闭环 |
| **ControlLogService** | 独立事务审计(REQUIRES_NEW) |
| **PlotService** | RBAC动态过滤 + CRUD |
| PlotOverview | 地块概览VO(含hasControllers) |
| AlertEngine | 动态规则加载 + 阈值扫描 |
| **SensorService** | 降采样(TimeGranularity四档) |

#### 模拟数据层 smart-agriculture-simulator (3)
| 类名 | 核心逻辑 |
|------|---------|
| DataCollector | 接口(Mock/MQTT切换) |
| **MockDataCollector** | 动态轮询 + saveBatch批量插入 |
| HeartbeatChecker | 3分钟心跳超时→OFFLINE |

---

## Phase 2 展望：后端全量实现

Phase 2 将完成以下内容，使后端可独立启动并通过 Postman 测试：

1. **Maven 工程搭建**：父POM + 7个子模块POM + Maven Wrapper
2. **Spring Boot 启动类**：SmartFarmApplication + application.yml (dev/mock/prod)
3. **数据库自动初始化**：启动时执行 schema.sql + data.sql
4. **JWT 认证体系**：Spring Security + JwtAuthFilter + AuthController
5. **全部 Controller 实现**：
   - AuthController (login/me)
   - PlotController (CRUD + RBAC)
   - DeviceController (list/bind/unbind)
   - SensorController (realtime/history)
   - ControlController (irrigation/log)
   - AlertController (rule/log/read)
   - AiController (chat/knowledge)
6. **DeviceService**：设备绑定/解绑 + 仓库管理
7. **AI 模块**：RagService + LlmService (规则/LLM双模式)
8. **WebSocket STOMP 配置**

---

## 关键设计决策记录 (供审核参考)

| 决策 | 选择 | 原因 |
|------|------|------|
| 事务策略 | 业务方法无@Transactional，日志用REQUIRES_NEW | 审计日志不可因业务异常丢失 |
| RBAC | ADMIN全局/FARMER owner过滤 | ADMIN需要管理所有地块 |
| 降采样 | 4档粒度(5min/1h/4h/1d) | 确保≤500数据点 |
| 调度线程池 | poolSize=10 | 采集/心跳/告警互不阻塞 |
| 批量插入 | saveBatch | 单次DB交互完成所有INSERT |
| 模拟数据 | 动态轮询(查DB获取设备) | 新设备绑定后无需重启 |
| 指令超时 | CompletableFuture 10s | 生产可替换为Redis分布式 |
| 前端图标 | Element Plus Icons / uni-icons / iconfont / MDI | 禁止emoji |

---

## 文件目录树 (当前状态)

```
smart-agriculture/
├── docs/superpowers/specs/
│   ├── 2026-07-01-smart-agriculture-design.md      (Phase 0: 设计规格说明书 v2.0)
│   ├── 2026-07-01-deliverables-summary.md           (Phase 0→1: 初次交付总结)
│   └── 2026-07-01-phase1-foundation-summary.md      (Phase 1: 本文档)
│
├── smart-agriculture-common/src/main/java/com/smartfarm/common/
│   ├── R.java
│   └── exception/
│       ├── BizException.java
│       └── GlobalExceptionHandler.java
│
├── smart-agriculture-framework/src/main/java/com/smartfarm/framework/
│   ├── config/
│   │   ├── SchedulingConfig.java                    (Phase 1 修复4)
│   │   └── MyBatisPlusConfig.java                   (Phase 1)
│   ├── security/
│   │   ├── UserContext.java
│   │   └── RbacUtils.java                           (Phase 1 修复2)
│   └── websocket/
│       └── WsPushService.java
│
├── smart-agriculture-business/src/main/java/com/smartfarm/business/
│   ├── entity/                                      (8个实体类)
│   │   ├── User.java
│   │   ├── Plot.java
│   │   ├── Device.java
│   │   ├── SensorData.java
│   │   ├── AlertRule.java
│   │   ├── AlertLog.java
│   │   ├── ControlLog.java
│   │   └── KnowledgeBase.java
│   ├── mapper/                                      (8个Mapper接口)
│   │   ├── UserMapper.java
│   │   ├── PlotMapper.java
│   │   ├── DeviceMapper.java
│   │   ├── SensorDataMapper.java
│   │   ├── ControlLogMapper.java
│   │   ├── AlertRuleMapper.java
│   │   ├── AlertLogMapper.java
│   │   └── KnowledgeBaseMapper.java
│   ├── control/
│   │   ├── ControlService.java                      (Phase 1 修复1)
│   │   └── ControlLogService.java                   (Phase 1 修复1)
│   ├── plot/
│   │   ├── PlotService.java                         (Phase 1 修复2)
│   │   └── PlotOverview.java
│   ├── monitor/
│   │   └── SensorService.java                       (Phase 1 修复3)
│   └── alert/
│       └── AlertEngine.java
│
├── smart-agriculture-simulator/src/main/java/com/smartfarm/simulator/
│   ├── DataCollector.java
│   ├── MockDataCollector.java                       (Phase 1 修复4)
│   └── HeartbeatChecker.java
│
└── smart-agriculture-server/src/main/resources/db/
    ├── schema.sql
    └── data.sql
```

---

## 审核清单 (供分析师检查)

请分析师重点审核以下内容：

1. **事务隔离**：ControlLogService 的 insertSentLog/markTimeout/markSuccess/markFailed 是否全部标注 `REQUIRES_NEW`？ControlService.executeCommand 是否已移除 `@Transactional`？
2. **RBAC**：RbacUtils.getOwnerFilterId() 对 ADMIN 返回 null 的逻辑是否被 PlotService 正确使用？
3. **降采样**：SensorService.TimeGranularity 四档粒度划分是否合理？MAX_DATA_POINTS=500 上限是否生效？
4. **线程池**：SchedulingConfig 的 poolSize=10 是否足够？CallerRunsPolicy 饱和策略是否合适？
5. **批量插入**：MockDataCollector 是否改为继承 ServiceImpl + saveBatch？是否保留了降级单条重试？
6. **实体字段**：Device.device_category 是否定义 SENSOR/CONTROLLER 枚举？ControlLog.command_status 是否定义了完整状态机？
7. **预置数据**：data.sql 的场景矩阵是否正确覆盖地块B无CONTROLLER和仓库未绑定设备两个场景？
