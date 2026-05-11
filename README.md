# CCEC 工位倒计时系统

基于 C++ + Java 的可部署工程版本，覆盖 PLC 采集、事件处理、状态机、WebSocket 推送、工位屏展示、Redis、Oracle 与 Nginx 入口。

## 目录结构

```text
cpp/plc-collector        C++ PLC 采集服务
java/timer-backend       Java Spring Boot 后端
frontend/station-screen  工位倒计时屏（现场显示终端，浏览器全屏）
frontend/admin-console   运营管理控制台（工位/CT/告警/生产/审计）
android/station-kiosk    安卓一体机（原生 MQTT + Room 离线缓存 + 设备遥测/指令，见 README）
deploy/nginx             Nginx 反向代理配置
deploy/scripts           一键启动/停止/日志脚本
docs                     架构、设计与部署文档
docker-compose.yml       本地/服务器容器化编排
```

## 快速部署

**首次全量搭建**（检查 Docker、生成 `.env`、构建启动、等待 Oracle/后端就绪并打印地址）：

```bash
chmod +x deploy/scripts/*.sh deploy/scripts/lib/*.sh
./deploy/scripts/setup-local.sh
# 或: make setup
```

日常启动：

```bash
./deploy/scripts/bootstrap.sh
# 或: make up
```

详细说明见 [docs/run-and-deploy.md](docs/run-and-deploy.md)。

**平台与终端部署分工、完备性对照、运维分层**：索引见 [docs/deployment-and-operations-overview.md](docs/deployment-and-operations-overview.md)；**投标简版（表格）**见 [docs/deployment-and-operations-overview-bid.md](docs/deployment-and-operations-overview-bid.md)；**实施详版**见 [docs/deployment-and-operations-overview-implementation.md](docs/deployment-and-operations-overview-implementation.md)。

Windows：

```powershell
Copy-Item .env.example .env
.\deploy\scripts\bootstrap.ps1
```

访问：

```text
http://localhost/?station=A601              # 工位屏（显示终端）
http://localhost/admin/index.html           # 运营管理控制台（先登录）
http://localhost/actuator/health
```

控制台默认账号（仅首次空库自动创建，**生产环境请立即改密并接入企业目录**）：

- 用户名：`admin`
- 初始密码：`Admin123!`

招标/方案对齐要点：**采集 → Redis Stream → Java 状态机 → WebSocket 工位屏**；**Oracle 主数据与 CT/生产/告警/审计**；**独立管理端 + 会话登录 + 配置变更审计**（目录登录可在 `DbUserDetailsService` 替换为 LDAP `UserDetailsService`）。

## 技术分工

- C++：PLC S7 只读采集、镜像 DB 解析、事件去重、断线重连、本地落盘缓冲。
- Java：CT 匹配、状态机、告警、REST 管理接口、WebSocket 推送、Oracle 持久化、审计。
- Redis：事件队列、实时状态缓存、推送通道、健康状态。
- Nginx：HTTP/HTTPS/WSS 统一入口。

## 说明

当前 C++ 采集端内置模拟事件，并将 JSON 事件写入 Redis Stream（`station:event:queue`，字段 `payload`），Java 端消费后驱动状态机、WebSocket 与 Oracle 记录。接入现场 PLC 时，在 `cpp/plc-collector/src/PlcClient.cpp` 中替换为厂商 SDK/Snap7 的只读采集实现，并保持与 `StationEvent` 相同的 JSON 字段。


## 生产级部署

```bash
cp .env.example .env
./deploy/scripts/prod-up.sh
```

生产增强内容：

- Prometheus + Alertmanager + Grafana 监控告警
- Loki + Promtail 日志检索
- Node Exporter + cAdvisor 主机/容器资源监控
- Oracle 备份与恢复脚本
- systemd 开机自启
- GitHub Actions CI/CD 自动构建与发布

详见：`docs/production-deployment.md`
