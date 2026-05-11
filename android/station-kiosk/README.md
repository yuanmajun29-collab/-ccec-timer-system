# CCEC 工位一体机 · Android Kiosk

## 能力概览

| 能力 | 说明 |
|------|------|
| **原生 MQTT** | Eclipse Paho 订阅 `{topicPrefix}/{工位}/snapshot`，与后端 `MqttStationPublisher` 发布的 JSON 快照一致（可与 WebSocket 并存）。 |
| **离线缓存** | Room 持久化最后一帧快照；断网或 Broker 不可达时展示缓存，横幅提示「离线缓存」。 |
| **设备管控** | 自动生成稳定 **deviceId**，每分钟向 `ccec/device/{deviceId}/telemetry` 发布遥测（版本、连接状态、快照延迟）。订阅 `ccec/device/{deviceId}/cmd` 接收 JSON 指令：`CLEAR_CACHE`、`RECONNECT`。 |

## 显示模式

- **原生 MQTT（默认）**：全屏原生 UI + 前台服务保持 MQTT；需配置 Broker（默认 `tcp://…:1883`）与 topic 前缀（与后端一致：`ccec/station`）。
- **WebView**：关闭「使用原生 MQTT」后，仅加载 `http(s)://服务器/?station=` 页面（与浏览器工位屏一致）。

## 与后端 / Docker

根目录 `docker-compose.yml` 已包含 **Mosquitto**，后端 `timer-backend` 在 `SPRING_PROFILES_ACTIVE=docker` 时默认打开 MQTT（`application-docker.yml`），向 `ccec/station/{工位}/snapshot` 发布 **retained** 快照。

真机访问容器映射的 **1883** 端口；模拟器访问宿主机 Docker 可用 `tcp://10.0.2.2:1883`。

## 构建

```bash
cd android/station-kiosk
./gradlew :app:assembleDebug
```

## 权限说明

- **前台服务**（`dataSync` 类型）：维持 MQTT 与遥测。
- **POST_NOTIFICATIONS**（Android 13+）：显示常驻通知；若系统拒绝，部分机型可能影响前台服务通知展示，请在系统设置中允许通知。

## 远程指令示例（MQTT 发布）

主题：`ccec/device/<设备ID>/cmd`  
载荷示例：

```json
{"cmd":"CLEAR_CACHE"}
```

```json
{"cmd":"RECONNECT"}
```

设备 ID 首次启动生成，可在遥测 JSON 的 `deviceId` 字段查看，也可后续扩展设置页展示。
