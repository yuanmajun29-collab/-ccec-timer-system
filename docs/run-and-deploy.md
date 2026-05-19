# 运行与部署环境（完整说明）

本文说明如何在**开发机或服务器**上从零搭建可运行的 CCEC 工位倒计时全栈环境，并与招标/方案中的组件对齐。

## 1. 架构与容器

| 服务 | 说明 |
|------|------|
| `redis` | 事件 Stream、工位状态缓存 |
| `oracle` | Oracle Free 23（开发/联调）；生产可换企业 Oracle |
| `timer-backend` | Spring Boot：状态机、管理 API、WebSocket、Flyway |
| `plc-collector` | C++ 采集（当前模拟 + Redis XADD） |
| `edgebox-gate` | 边缘统一入口（Nginx 实现）、工位屏静态页、管理控制台、`/api` `/ws` 反代 |

生产叠加 `docker-compose.prod.yml` 时另含：Prometheus、Grafana、Loki、Alertmanager、exporters 等。

## 2. 本机前置条件

- **Docker** 20.10+（推荐 Docker Desktop for Mac/Windows，或 Linux `docker.io` + `docker compose` 插件）
- **内存** 建议 ≥ 8GB（Oracle 容器占用较高）
- **磁盘** 预留 ≥ 15GB（镜像 + 卷）
- **curl**（用于健康检查脚本；macOS/Linux 一般已有）
- 端口默认可访问：**80**（HTTP）、**8080**（后端直连）、**1521**（Oracle）、**6379**（Redis）。被占用时请在 `.env` 中修改 `HTTP_PORT`、`BACKEND_PORT` 等。

## 3. 一键搭建（推荐）

在项目根目录执行：

```bash
chmod +x deploy/scripts/*.sh deploy/scripts/lib/*.sh
./deploy/scripts/setup-local.sh
```

或使用 Makefile：

```bash
make setup
```

脚本将依次：**前置检查** → 复制 `.env`（若不存在）→ **`docker compose up -d --build`** → **等待后端 `/actuator/health` 为 UP**（最长约 10 分钟，首次拉 Oracle 镜像较慢）→ **打印访问地址**。

仅日常启动（已构建过镜像、环境已就绪）可用：

```bash
make up
# 或
./deploy/scripts/bootstrap.sh
```

## 4. Windows（PowerShell）

需已安装 Docker Desktop，并在项目根目录执行：

```powershell
Copy-Item .env.example .env -ErrorAction SilentlyContinue
docker compose up -d --build
docker compose ps
```

健康检查（需手动等待后端启动完成）：

```powershell
$port = 8080
for ($i = 0; $i -lt 120; $i++) {
  try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:$port/actuator/health" -UseBasicParsing -TimeoutSec 5
    if ($r.Content -match '"status":"UP"') { Write-Host "Backend OK"; break }
  } catch {}
  Start-Sleep -Seconds 3
}
```

然后浏览器打开：`http://localhost/?station=A601` 与 `http://localhost/admin/login.html`。

## 5. 生产环境（含监控）

```bash
./deploy/scripts/prod-up.sh
# 或
make prod
```

会合并 `docker-compose.yml` 与 `docker-compose.prod.yml`。首次请配置 `.env` 中 `REGISTRY` / `IMAGE_NAMESPACE` / `IMAGE_TAG`、Grafana 密码、SMTP 等。

## 6. 常用运维

```bash
docker compose ps
./deploy/scripts/logs.sh
./deploy/scripts/logs.sh timer-backend
./deploy/scripts/logs.sh edgebox-gate
./deploy/scripts/stop.sh
./deploy/scripts/print-endpoints.sh
```

## 7. 故障排查

| 现象 | 处理 |
|------|------|
| `prerequisites.sh` 报 Docker 不可用 | 启动 Docker Desktop / `sudo systemctl start docker`，确认当前用户在 `docker` 组 |
| 端口冲突 | 修改 `.env` 中 `HTTP_PORT`、`BACKEND_PORT`、`ORACLE_PORT`、`REDIS_PORT` |
| 后端一直不健康 | `docker compose logs timer-backend`；常见为 Oracle 仍在首次初始化（可再等 5～10 分钟） |
| Flyway 失败 | 确认 Oracle 应用用户与 `SPRING_DATASOURCE_*` 一致；勿手动删迁移版本表导致不一致 |
| 工位屏无数据 | 确认 `plc-collector` 在跑且 Redis Stream 有事件；见 `docker compose logs plc-collector` |

## 8. 与现场 Oracle / HTTPS 的衔接

- **替换 Oracle**：改 `.env` 与 `timer-backend` 的 `SPRING_DATASOURCE_URL`（生产 compose 可通过环境变量注入），不再依赖 `oracle` 服务时可从 compose 中移除该服务并调整 `depends_on`。
- **HTTPS/WSS**：在 edgebox-gate 上配置证书（`deploy/edgebox-gate/certs` 已在 prod compose 中挂载占位），并统一使用 `https://` 访问；工位屏 WebSocket 会自动使用 `wss:`。

更简版说明见：`docs/deployment.md`。
