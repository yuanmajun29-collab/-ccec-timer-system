# 系统架构

依据 **docs/V8.4.1** 技术方案与 **概要设计 V1.2 修正版** 的七层工业架构表述（与代码实现对齐）。

```text
L1 PLC/RFID → L3 C++ 采集（只读）→ L4 Redis Stream（默认 stream:station:event）
    → L5 Java（状态机、CT、批量 Oracle）→ L6 WSS/MQTT（STATE_UPDATE）→ L7 工位屏 / 管理端
                                      ↘ Oracle / 审计
```

## 要点（与 V1.2 一致）

1. **网络隔离**：VLAN/ACL/ARP 等由工业交换机/安全网关承担；工控机不充当跨网段路由器（V1.2 §1.3）。
2. **Redis**：事件流 + 工位快照 Hash（默认 `hash:station:state`）；单机 Redis + AOF 为合同基线，Sentinel 为选配。
3. **工位推送**：WebSocket 路径 `/ws/station/{stationCode}`；对外 JSON 使用 `STATE_UPDATE`（`soNo`/`esnNo`/`state`/`ct`/`remain` 等）。
4. **状态机**：六态含 `ABNORMAL`；未知机型无 CT 配置时不启动倒计时；`abnormalCode != 0` 时强制异常态（见 `StationStateMachine`）。
5. **CT 阈值**：黄/红区由 `T_CT_CONFIG.WARN_THRESHOLD` / `ALARM_THRESHOLD`（已用/标准 CT 比例）驱动，Flyway `V4` 将历史种子从 0.7/0.9 对齐到 0.5/0.8。

## 模块边界

1. C++ 采集服务只读访问 PLC TCP 102，不写 PLC。
2. C++ 输出标准 `StationEvent`（含 `abnormalCode`），不做业务 CT 判定。
3. Java 消费事件，执行 CT 匹配、状态机、告警与推送。
4. Oracle 写入由 Java 数据服务统一处理，采集服务不直接大量写库。
