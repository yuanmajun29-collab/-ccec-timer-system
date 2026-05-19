# 部署与运维总览 · 投标简版（纯表格）

| 属性 | 说明 |
|------|------|
| **文档用途** | 投标/方案附件：**边界、组件、端口、终端形态、运维分层**一目了然 |
| **建议受众** | **甲方 IT 负责人、招标评审、项目经理**（不重技术命令细节） |
| **篇幅提示** | 下文均为表格；导出 Word/A4、默认页边距、正文 **小四** 时约 **2～3 页**（随表格换行略浮动） |
| **详细展开** | 见 `deployment-and-operations-overview-implementation.md` |

---

## 1. 方案能力与交付对照（摘要）

| 能力域 | 交付状态 | 备注 |
|--------|----------|------|
| PLC 只读采集 → Redis Stream → Java | 主干已实现 | C++ 现场替换为 Snap7/SDK |
| 状态机 / CT（Oracle 配置优先） | 已实现 | — |
| 工位实时展示（浏览器 WebSocket） | 已实现 | `/?station=` |
| 工位实时展示（MQTT + 安卓原生） | 已实现 | 与 WS 并行；需 Broker |
| Oracle 主数据 / CT / 生产 / 告警 / 审计 | 已实现 | `T_EVENT_LOG` 方案提及但未建表 |
| 运营管理端 + 登录 + 审计写入 | 已实现 | LDAP 为后续增强 |
| 生产监控（Prometheus/Grafana/Loki 等） | Compose 可选栈 | 见 `production-deployment.md` |
| 班次 / 声光硬件联动 / AD | 未闭环 | 合同二期或变更项 |

---

## 2. 职责边界（避免验收争议）

| 层级 | 职责 | 不负责 |
|------|------|--------|
| **平台（机房）** | 采集、计算、入库、统一入口、WS/MQTT 发布、备份与监控 | 车间交换机 VLAN 细节、PLC 编程 |
| **显示终端（车间）** | 展示、本地缓存（安卓）、遥测上报 | 业务规则、数据库 |
| **C++ 采集** | 只读 PLC、写 Redis | 写 PLC、直连 Oracle 大量写库 |

---

## 3. 平台侧组件与端口（Docker 默认）

| 组件 | 作用 | 典型端口 |
|------|------|----------|
| edgebox-gate（Nginx） | 工位 H5、管理端、`/api`、`/ws`、入口清单 | 80 / 443 |
| timer-backend | 业务、Flyway、WS、REST、MQTT 发布 | 8080 |
| redis | Stream、状态缓存 | 6379 |
| oracle | 数据存储（生产多为企业库） | 1521 |
| plc-collector | PLC → Redis | — |
| mosquitto | MQTT Broker | 1883 |

---

## 4. 显示终端形态与连接

| 形态 | 访问方式 | 车间需放行（指向机房） |
|------|----------|------------------------|
| 浏览器全屏 | `http(s)://平台/?station=工位` + WebSocket | **80/443** |
| 安卓 APK（原生 MQTT） | Broker `tcp(s)://…:1883` + topic 前缀与后端一致 | **1883**（及 80/443 若用 WebView 模式） |

| 约束 | 说明 |
|------|------|
| 终端 **不** 直连 Oracle / Redis | 降低暴露面 |
| HTTPS 入口 | 浏览器自动 **wss** |

---

## 5. 运维分层（甲方 IT 管理视图）

| 层级 | 对象 | 典型手段 |
|------|------|----------|
| 基础设施 | 主机、Docker、磁盘 | compose ps、监控 exporter |
| 应用 | Java、Redis、Oracle、edgebox-gate | 健康检查、日志 |
| 业务链路 | PLC→采集→Stream→后端→屏 | 分段日志与队列/MQTT 抽检 |
| 数据与安全 | 备份、账号、审计 | 备份脚本、管理端操作留痕 |

---

## 6. 例行巡检（最低限度）

| 频率 | 检查项 |
|------|--------|
| 每日 | 容器存活、磁盘、备份成功、后端 ERROR 日志 |
| 每周 | 监控面板、Redis/Oracle 资源（若自建库） |
| 变更后 | 迁移版本、关键工位屏/MQTT 抽样 |

---

## 7. 文档关系

| 文档 | 受众 |
|------|------|
| 本文（投标简版） | 甲方 IT / 招标 |
| `deployment-and-operations-overview-implementation.md` | 实施交付 / **车间运维** / 集成商驻场 |
| `run-and-deploy.md`、`production-deployment.md`、`runbooks/common-operations.md` | 执行命令与脚本级细节 |
