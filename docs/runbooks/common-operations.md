# 运维手册

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
2. 检查 Redis 连接。
3. 检查 WebSocket 是否被 Nginx 正确转发。
4. 现场联调时确认 PLC IP、rack、slot 与 DB 地址配置。
