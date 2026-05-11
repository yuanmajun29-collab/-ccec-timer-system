# CCEC 工位倒计时系统：生产级部署方案

## 目标

本方案面向生产环境，补齐以下能力：

- Docker Compose 双文件编排：基础服务 + 生产增强。
- Prometheus 指标采集、Alertmanager 告警、Grafana 看板。
- Loki + Promtail 日志集中检索。
- cAdvisor + Node Exporter 采集容器与主机资源。
- Oracle 数据泵备份/恢复脚本。
- systemd 开机自启。
- GitHub Actions 自动构建、测试、推送镜像与 SSH 部署。

## 生产部署命令

```bash
cp .env.example .env
vi .env
./deploy/scripts/prod-up.sh
```

访问入口：

```text
业务入口:      http://<server>/
后端健康检查:  http://<server>/actuator/health
Grafana:       http://<server>:3000
Prometheus:    http://<server>:9090
Alertmanager:  http://<server>:9093
```

## systemd 开机自启

```bash
sudo mkdir -p /opt/ccec-timer
sudo cp -r . /opt/ccec-timer/ccec-timer-system
sudo cp deploy/systemd/ccec-timer.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ccec-timer
sudo systemctl start ccec-timer
```

## 备份与恢复

备份：

```bash
./deploy/scripts/backup-oracle.sh
```

恢复：

```bash
./deploy/scripts/restore-oracle.sh /opt/ccec-timer/backups/ccec_YYYYmmdd_HHMMSS.dmp
```

建议将备份脚本加入 crontab：

```cron
0 2 * * * /opt/ccec-timer/ccec-timer-system/deploy/scripts/backup-oracle.sh >> /var/log/ccec-oracle-backup.log 2>&1
```

## CI/CD 配置

### Actions Secrets（SSH 部署用）

```text
PROD_HOST       生产服务器 IP 或域名
PROD_USER       SSH 用户
PROD_SSH_KEY    SSH 私钥
PROD_SSH_PORT   SSH 端口，可选
```

### Actions Variables（控制是否自动部署）

在 **Settings → Secrets and variables → Actions → Variables** 新增：

| 变量名 | 值 | 说明 |
|--------|-----|------|
| `ENABLE_SSH_DEPLOY` | `true` | 仅在 **push main** 且为 `true` 时执行 SSH 部署；未设置或非 `true` 时 **跳过部署**，仍构建并推送镜像到 GHCR |

`if` 中不能使用 `secrets`，故用变量显式开关，避免未配密钥时 **main** 流水线整单失败。

### 流水线行为

1. **pull_request → main**：Java 测试、Docker Compose 校验、Android Debug 构建、本地 `docker build` 镜像（不推送 GHCR）。
2. **push main**：同上 + 推送镜像到 GHCR；**仅当** `ENABLE_SSH_DEPLOY == true` 时执行 SSH `prod-up`。
3. **tag v\***：生成对应版本镜像并推送。

## 监控告警

内置告警：

- Java 后端不可用。
- Redis 不可用。
- 主机 CPU 使用率超过 85%。
- 主机内存使用率超过 90%。
- 磁盘使用率超过 85%。

SMTP 参数在 `.env` 中配置。生产环境建议接入企业邮箱、钉钉/飞书/企业微信 Webhook 或统一告警平台。

## 安全建议

- 修改 `.env` 中所有默认密码。
- 生产环境只开放 80/443，Grafana/Prometheus/Alertmanager 建议仅内网或 VPN 访问。
- HTTPS 证书放置在 `deploy/nginx/certs`，并更新 Nginx 配置。
- Oracle、Redis 不建议暴露公网端口。
- 定期验证备份文件可恢复。
