# CCEC 工位倒计时系统

基于 C++ + Java 的可部署工程版本，覆盖 PLC 采集、事件处理、状态机、WebSocket 推送、工位屏展示、Redis、Oracle 与 Nginx 入口。

## 目录结构

```text
cpp/plc-collector        C++ PLC 采集服务
java/timer-backend       Java Spring Boot 后端
frontend/station-screen  工位倒计时屏静态页面
deploy/nginx             Nginx 反向代理配置
deploy/scripts           一键启动/停止/日志脚本
docs                     架构、设计与部署文档
docker-compose.yml       本地/服务器容器化编排
```

## 快速部署

```bash
cp .env.example .env
./deploy/scripts/bootstrap.sh
```

Windows：

```powershell
Copy-Item .env.example .env
.\deploy\scripts\bootstrap.ps1
```

访问：

```text
http://localhost/?station=A601
http://localhost/actuator/health
```

## 技术分工

- C++：PLC S7 只读采集、镜像 DB 解析、事件去重、断线重连、本地落盘缓冲。
- Java：CT 匹配、状态机、告警、REST 管理接口、WebSocket 推送、Oracle 持久化、审计。
- Redis：事件队列、实时状态缓存、推送通道、健康状态。
- Nginx：HTTP/HTTPS/WSS 统一入口。

## 说明

当前 C++ 采集端内置模拟事件，便于后端和工位屏部署联调。接入现场 PLC 时，需要在 `cpp/plc-collector/src/PlcClient.cpp` 中替换为厂商 SDK/Snap7 的只读采集实现。


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
