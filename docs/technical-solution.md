# CCEC 工位倒计时系统技术方案与设计方案

## 1. 总体架构

系统采用“PLC 只读采集 + Redis 事件队列 + Java 状态机 + **WebSocket / MQTT** 工位展示 + 运营管理端”的架构（实现细节与运维边界见 `docs/deployment-and-operations-overview.md`）。

```text
PLC 镜像 DB
   ↓ 只读采集
C++ plc-collector
   ↓ 事件发布
Redis Stream
   ↓ 消费/缓存
Java timer-backend
   ↓ WebSocket / MQTT(retained 快照) / REST
edgebox-gate(Nginx) + Mosquitto(可选 Broker)
   ↓
工位浏览器屏 / 安卓一体机(APK) / 管理端
```

## 2. 模块设计

### C++ 采集端

- 连接 PLC S7 TCP 或厂商 SDK。
- 周期读取 PLC 镜像 DB。
- 解析工位到达、离开、暂停、返修、旁通等信号。
- 生成 `StationEvent`。
- 发布到 Redis Stream（默认键名 `station:event:queue`，与采集端 `REDIS_STREAM_KEY`、后端 `TIMER_REDIS_STREAM_KEY` / `timer.redis.stream-key` 对齐，可用环境变量覆盖）。
- 断线重连，异常事件本地落盘。

当前工程提供模拟采集实现，方便容器化部署联调。

### Java 后端

- Spring Boot 提供 REST API、WebSocket 和状态机服务。
- 根据工位事件维护 `StationContext`。
- 计算标准 CT、已用时间、剩余时间和显示颜色。
- 推送 `STATE_UPDATE` JSON（与 V1.2 §3.8 字段一致）及 Redis 快照缓存到对应工位屏；MQTT 使用相同载荷。
- 持久化工位、CT配置、生产记录、审计日志。

### 工位屏

- 静态 HTML 页面。
- 通过 `?station=A601` 指定工位。
- 建立 `/ws/station/{stationCode}` WebSocket。
- 实时显示倒计时、SO、ESN、机型、状态。

## 3. 数据设计

核心表：

- `T_STATION`：工位主数据。
- `T_CT_CONFIG`：CT 标准配置。
- `T_PRODUCTION_RECORD`：生产过程记录。
- `T_EVENT_LOG`：PLC 事件日志。
- `T_AUDIT_LOG`：操作审计日志。

DDL 位于：

```text
java/timer-backend/src/main/resources/db/migration/V1__init_oracle.sql
```

## 4. 部署设计

容器服务：

- Redis：事件队列和实时缓存。
- Oracle Free：开发/测试数据库。
- timer-backend：Java 服务。
- plc-collector：C++ 采集服务。
- edgebox-gate：统一入口（Nginx 实现）。

生产环境可替换 Oracle 为现场正式库，并将 Redis 升级为主从/哨兵模式。

## 5. 安全设计

- PLC 采集必须只读，不允许写 DB 或写变量。
- 管理接口后续接入 AD/LDAP。
- 所有配置变更写入审计日志。
- edgebox-gate 生产环境启用 HTTPS/WSS。
- Redis 和 Oracle 生产环境启用强密码和网络隔离。

## 6. 开发落地计划

1. 完成 PLC 镜像 DB 地址表确认。
2. 替换 C++ 模拟采集为真实 PLC SDK 采集。
3. 完成 Redis Stream 消费链路。
4. 完成 CT 配置、班次配置、工位管理接口。
5. 完成告警策略和声音策略。
6. 完成 AD/LDAP 登录和审计。
7. 完成现场 40 个工位屏联调。
8. 完成压测、断网恢复测试和上线验收。
