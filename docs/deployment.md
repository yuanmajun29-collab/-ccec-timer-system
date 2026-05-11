# CCEC 工位倒计时系统部署方案

## 部署组件

- `nginx`：统一入口，提供工位屏静态页面、REST 反向代理、WebSocket 转发。
- `timer-backend`：Java Spring Boot 后端，负责状态机、接口、WebSocket 推送、Oracle 持久化。
- `plc-collector`：C++ PLC 采集服务，按只读方式采集 PLC 镜像 DB，向事件队列发布事件。
- `redis`：事件队列、实时缓存、服务健康状态。
- `oracle`：生产数据、CT 配置、审计日志、操作记录。

## 一键启动

```bash
cp .env.example .env
# 修改 .env 内 PLC_HOST、端口和数据库密码
./deploy/scripts/bootstrap.sh
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
.\deploy\scripts\bootstrap.ps1
```

## 访问地址

- 工位屏：`http://服务器IP/?station=A601`
- 后端健康检查：`http://服务器IP/actuator/health`
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
2. Nginx 生产环境应启用 HTTPS，并配置企业证书。
3. PLC 上线前必须将 C++ `PlcClient` 的模拟采集替换为 Snap7/厂商 SDK 只读采集实现。
4. Redis 生产环境建议启用密码、持久化和主从/哨兵。
5. 所有容器日志接入统一日志平台，保留操作审计和告警记录。
