# 运维手册

整体分层、平台与终端边界：索引 **[deployment-and-operations-overview.md](../deployment-and-operations-overview.md)**；投标简版 **[deployment-and-operations-overview-bid.md](../deployment-and-operations-overview-bid.md)**；实施详版 **[deployment-and-operations-overview-implementation.md](../deployment-and-operations-overview-implementation.md)**。

## 查看服务状态

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

## 查看日志

```bash
./deploy/scripts/prod-logs.sh timer-backend
./deploy/scripts/prod-logs.sh plc-collector
```

## 重启单个服务

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml restart timer-backend
```

## 发布新版本

```bash
git pull --ff-only
IMAGE_TAG=latest ./deploy/scripts/prod-up.sh
```

## 常见问题

### 后端健康检查失败

1. 检查 Oracle 是否健康。
2. 检查 Redis 是否健康。
3. 查看后端日志。
4. 确认 `.env` 中数据库账号密码一致。

### 工位屏无数据

1. 检查 PLC 采集端日志。
2. 检查 Redis 连接与 Stream 是否有新消息。
3. **浏览器屏**：检查 WebSocket 是否被 Nginx 正确转发（`/ws/` Upgrade）。
4. **安卓原生 MQTT**：检查 Mosquitto 是否可达（默认 **1883**）、`TIMER_MQTT_ENABLED`、topic 前缀与一体机设置一致；可用 `mosquitto_sub` 订阅 `ccec/station/<工位>/snapshot` 验证后端是否在发布。
5. 现场联调时确认 PLC IP、rack、slot 与镜像 DB 映射。

### MQTT 无消息（仅原生终端）

1. `docker compose logs mosquitto`、`timer-backend` 是否含 MQTT 连接成功日志。
2. `.env` 中 `TIMER_MQTT_BROKER_URI`（容器内一般为 `tcp://mosquitto:1883`）。
3. 防火墙放行车间网 → 机房 **1883**（若终端跨网段）。
