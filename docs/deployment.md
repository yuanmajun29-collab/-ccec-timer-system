# CCEC 工位倒计时系统部署方案

## 部署组件

- `edgebox-gate`：统一边缘入口（Nginx 实现），提供工位屏静态页面、REST 反向代理、WebSocket 转发与入口清单。
- `timer-backend`：Java Spring Boot 后端，负责状态机、接口、WebSocket 推送、Oracle 持久化；可选向 **MQTT** 发布工位快照（与安卓原生终端对齐）。
- `mosquitto`：MQTT Broker（默认 1883），用于一体机原生订阅 `ccec/station/{工位}/snapshot`。
- `plc-collector`：C++ PLC 采集服务，按只读方式采集 PLC 镜像 DB，向事件队列发布事件。
- `redis`：事件队列、实时缓存、服务健康状态。
- `oracle`：生产数据、CT 配置、审计日志、操作记录。

## 一键启动

完整步骤、前置条件与排障见：**[run-and-deploy.md](./run-and-deploy.md)**。

```bash
chmod +x deploy/scripts/*.sh deploy/scripts/lib/*.sh
./deploy/scripts/setup-local.sh
# 或: make setup
```

日常启动（环境已就绪）：

```bash
./deploy/scripts/bootstrap.sh
# 或: make up
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
.\deploy\scripts\bootstrap.ps1
```

## 访问地址

- 工位屏：`http://服务器IP/?station=A601`
- 管理控制台：`http://服务器IP/admin/login.html`
- 网关健康检查：`http://服务器IP/healthz`
- 入口清单：`http://服务器IP/edgebox-gate/manifest.json`
- 后端健康检查：`http://服务器IP/actuator/health`（经 edgebox-gate）或直连 `BACKEND_PORT`
- 工位接口：`http://服务器IP/api/v1/stations`

## 常用运维命令

```bash
docker compose ps
./deploy/scripts/logs.sh
./deploy/scripts/logs.sh timer-backend
./deploy/scripts/stop.sh
```

## 生产部署建议

1. Oracle 可替换为工厂现有 Oracle，修改 `.env` 与 `SPRING_DATASOURCE_URL`。
2. edgebox-gate 生产环境应启用 HTTPS，并配置企业证书。
3. PLC 上线前必须将 C++ `PlcClient` 的模拟采集替换为 Snap7/厂商 SDK 只读采集实现。
4. Redis 生产环境建议启用密码、持久化和主从/哨兵。
5. 所有容器日志接入统一日志平台，保留操作审计和告警记录。
