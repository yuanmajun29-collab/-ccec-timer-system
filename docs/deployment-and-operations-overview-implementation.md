# 平台部署、显示终端部署与整体运维 · 实施详版

| 属性 | 说明 |
|------|------|
| **文档用途** | 交付实施、驻场集成、运维交接：**完备性说明、部署边界、运维分层、故障路径、引用索引** |
| **建议受众** | **甲方 IT（实施/运维）**、**车间运维值班**、**系统集成商驻场**（需配合命令与脚本） |
| **篇幅提示** | 导出 Word/A4、默认页边距、正文 **小四** 时约 **8～12 页**（含本节叙事与列表；不含附录全文粘贴） |
| **精简表格版** | 投标与管理层阅读：`deployment-and-operations-overview-bid.md` |

本文在技术方案（`technical-solution.md`）、架构说明（`architecture.md`）与当前仓库实现基础上撰写。**执行级命令**仍以 `docs/run-and-deploy.md`、`docs/production-deployment.md`、`docs/runbooks/common-operations.md` 为准。

---

## 一、开发完备性对照（相对技术方案 / 设计文档）

### 1.1 已对齐或可交付的核心能力

| 方案要点 | 仓库实现位置 | 说明 |
|----------|----------------|------|
| PLC → 采集 → Redis Stream → Java | `cpp/plc-collector`、`RedisEventConsumer`、`StationEventStreamPoller` | C++ 现场需替换为 Snap7/SDK；采集发布 **XADD `payload` JSON** |
| 状态机 + CT（DB 优先） | `StationStateMachine`、`JdbcCtConfigResolver`、`T_CT_CONFIG` | Oracle 返回标准 CT 与黄/红阈值；无配置则 ABNORMAL（V1.2） |
| WebSocket 工位屏 | `StationWebSocketHandler`、`frontend/station-screen` | `/?station=` + `/ws/station/{code}` |
| Oracle 主数据 / 生产 / 告警 / 审计表 | Flyway `V1`～`V3`、`ProductionRecordRepository`、`AlarmRecordRepository`、`AuditLogService` | **方案中的 `T_EVENT_LOG` 未建表**，PLC 原始事件若需落库需后续 DDL |
| 运营管理端 + 登录 | `frontend/admin-console`、Spring Security、管理 API `/api/v1/management/**` | 会话 + CSRF；默认管理员仅空库初始化 |
| MQTT 一体机原生通道 | `MqttStationPublisher`、`mosquitto`、`android/station-kiosk` | 与 WS **并行**；topic：`{prefix}/{工位}/snapshot` |
| 终端离线缓存 / 遥测 / 指令 | Android Room、`telemetry`、`cmd` | `CLEAR_CACHE`、`RECONNECT` 等 |
| 生产可观测性 | `docker-compose.prod.yml`、Prometheus/Grafana/Loki 等 | 见 `docs/production-deployment.md` |

### 1.2 部分实现或占位（招标/方案常写但代码未闭环）

| 项 | 现状 | 建议后续 |
|----|------|----------|
| **班次配置** | 无独立表与 API | 扩展配置表 + 状态机按班次取 CT |
| **声音策略 / 声光联动** | DB 字段、`soundPolicy` 未驱动硬件或前端 | 终端 APK 或边缘网关订阅 MQTT 策略 topic |
| **AD/LDAP** | 仍为内置账号 + `DbUserDetailsService` | 替换 `UserDetailsService` 或 OAuth2/OIDC |
| **T_EVENT_LOG（方案提及）** | DDL 未包含 | 增加迁移脚本 + 采集或 Java 批量写入策略 |
| **Hold 累计秒数** | `StationContext.holdSeconds` 未随事件维护 | 按 PLC 节拍事件增量更新 |
| **管理端报表导出** | 仅分页查询 | 导出 CSV/对接 BI |

### 1.3 边界声明（避免验收争议）

- **业务 CT、告警入库、审计**：以 **Java + Oracle** 为准；C++ **不写库**、**不写 PLC**。
- **实时展示**：浏览器与安卓均可；浏览器走 **HTTP(S)+WS**，安卓可走 **MQTT**（需 Broker 与后端发布开关一致）。
- **一体机 APK**：为 **显示与会话壳 + MQTT 客户端**，不替代服务端逻辑。

---

## 二、部署边界：平台侧 vs 显示终端侧

### 2.1 平台侧（服务端 / 边缘机房）

**职责**：事件接入、业务计算、持久化、对外统一入口、可选 MQTT 发布、监控与备份。

**典型组件（Docker Compose）**

| 组件 | 角色 | 默认端口（可改 `.env`） |
|------|------|-------------------------|
| **nginx** | 静态工位页、管理端、`/api`、`/ws` 反代 | HTTP **80**（prod 可挂 **443**） |
| **timer-backend** | Spring Boot、Flyway、WS、REST、MQTT 发布 | **8080**（直连健康检查） |
| **redis** | Stream 队列、工位状态缓存 key | **6379** |
| **oracle** | 开发/联调用镜像；生产常换为企业 Oracle | **1521** |
| **plc-collector** | PLC 只读采集 → Redis | 无对外 HTTP |
| **mosquitto** | MQTT Broker（安卓原生 / 后端发布） | **1883** |

**依赖顺序（逻辑）**：Redis、Oracle 就绪 → Java 启动并完成迁移 → Nginx 依赖后端健康；采集依赖 Redis；**MQTT** 可与后端并行，后端内置重连。

**配置要点**

- `.env`：`PLC_*`、`ORACLE_*`、`HTTP_PORT`、`TIMER_MQTT_*`、`MQTT_PORT` 等。
- 生产：`docker-compose.yml` + `docker-compose.prod.yml`，镜像仓库与 `IMAGE_*` 见 `production-deployment.md`。

**不包含**：车间每台一体机的安卓设置、浏览器主页书签、车间交换机 VLAN（仅建议在文档中约定网络可达性）。

---

### 2.2 显示终端侧（工位一体机 / 大屏）

**职责**：仅 **展示** 与 **本地策略壳**（缓存、可选遥测）；**不承载业务规则与数据库**。

两种形态 **二选一或并存（不同工位）**：

| 形态 | 入口 / 连接 | 配置要点 | 适用 |
|------|-------------|----------|------|
| **浏览器全屏** | `http(s)://<平台>/` + `?station=工位码`；WS `wss://.../ws/station/{code}` | 与平台同域或反向代理；HTTPS 时自动 **wss** | 已有浏览器的一体机、快速上线 |
| **安卓 Kiosk APK** | 原生 MQTT：`tcp(s)://<Broker>:1883`；topic 前缀与后端一致（默认 `ccec/station`）；可选 WebView 模式仅 HTTP | `android/station-kiosk` 内设置页；模拟器连宿主机 Docker 可用 `10.0.2.2` | 弱网缓存、统一运维 APK、遥测 |

**网络边界**

- 终端 → 平台：**80/443**（浏览器）、**8080**（直连调试）、**1883**（MQTT，若终端走原生）。
- 终端 **无需** 直连 Oracle/Redis；**禁止** 将数据库端口暴露到车间网。

**验收清单（终端单独）**

- [ ] 能解析目标工位码，且与 PLC/画面编号一致  
- [ ] 浏览器：WS 断线重连或刷新策略明确  
- [ ] 安卓：Broker、前缀、账号（若启用）与机房 Mosquitto 策略一致；通知权限允许（前台服务）

**车间运维关注点（安卓）**

- 一体机网络：`ping` 机房平台 IP；原生 MQTT 需 **1883** 可达（或 TLS 对应端口）。  
- APK：版本升级后仍指向正确 Broker 与 `topic` 前缀；通知权限被关闭时可能影响前台服务提示（依 ROM 而异）。  
- 详细：`android/station-kiosk/README.md`。

---

## 三、整体运维方案

### 3.1 运维分层

| 层级 | 对象 | 主要工具 / 脚本 |
|------|------|------------------|
| **基础设施** | 主机、Docker、磁盘、网络 | `docker compose ps`、节点 exporter、cAdvisor |
| **应用健康** | Java、Redis、Oracle、Nginx | `/actuator/health`、Redis ping、Oracle 连接 |
| **业务链路** | PLC → 采集 → Stream → 后端 → WS/MQTT → 屏 | 日志关键字、`stream:station:event`、`hash:station:state`、订阅 MQTT 测试 |
| **数据与安全** | Oracle 备份、账号、审计 | `backup-oracle.sh`、`T_AUDIT_LOG`、管理端改密 |
| **观测与告警** | 指标、日志、告警路由 | Prometheus、Grafana、Loki、Alertmanager（prod 栈） |

### 3.2 例行巡检（建议）

| 频率 | 内容 |
|------|------|
| 每日 | 容器状态、磁盘、`timer-backend` 日志 ERROR、备份任务是否成功 |
| 每周 | Grafana 面板浏览、Redis 内存、Oracle 表空间（若自建库） |
| 变更后 | Flyway 版本、MQTT 订阅抽样、关键工位屏抽样 |

### 3.3 故障定位路径（简版）

1. **全屏无刷新**：采集日志 → Redis Stream 是否有新消息 → Java 消费与 WS/MQTT 是否发布 → Nginx `/ws` 或 Mosquitto ACL。  
2. **仅安卓异常**：对比浏览器同工位；查 APK Broker 地址与 **TLS**；查 `telemetry` topic 是否上报。  
3. **管理端 401/403**：会话过期、CSRF；LDAP 未上线时为本地账号策略。  
4. **Oracle / Flyway**：先看后端启动日志与连接串一致性。

详细命令见 **`docs/runbooks/common-operations.md`**（含 MQTT 专项），生产组件见 **`docs/production-deployment.md`**。

### 3.4 变更与发布

- **CI**：`main` 合并构建镜像；部署脚本拉取镜像并 `prod-up`（见仓库 `.github/workflows/ci-cd.yml`）。  
- **配置变更**：优先走管理 API + 审计；数据库手工变更需补审计说明。  
- **终端 APK**：版本升级与服务器 **无强耦合**（H5 模式）；MQTT 模式需兼容 snapshot JSON 字段。

### 3.5 安全与合规（运维视角）

- 车间网 MQTT：**默认匿名仅限联调**；生产启用密码/TLS 并收紧 `network_security_config`。  
- **HTTPS/WSS**：生产入口强制 TLS，与方案一致。  
- **PLC**：运维不负责改 PLC 程序；仅确认采集侧 **只读** 与防火墙放行 **TCP 102**（S7）。

---

## 四、文档索引

| 文档 | 用途 |
|------|------|
| `deployment-and-operations-overview-bid.md` | 投标简版（纯表格） |
| `docs/run-and-deploy.md` | 本地/服务器 Compose 一键搭建、前置条件 |
| `docs/deployment.md` | 组件列表与快速命令 |
| `docs/production-deployment.md` | 监控栈、备份、systemd、CI/CD |
| `docs/runbooks/common-operations.md` | 日常运维命令与常见问题 |
| `android/station-kiosk/README.md` | 一体机 MQTT / 缓存 / 遥测 |

---

**结论**：当前实现已覆盖方案主线 **采集 → Redis → Java → Oracle + 双通道推送（WS / MQTT）+ 管理端 + 安卓增强**；方案中 **班次、声光、LDAP、PLC 事件日志表** 等仍需按合同增量迭代。部署上请严格区分 **机房平台栈** 与 **车间终端配置**，运维上按 **基础设施 → 应用 → 链路 → 数据** 分层值守。
