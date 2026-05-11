# 系统架构

```text
PLC/RFID -> C++ PLC Collector -> Redis Stream -> Java Timer Backend -> WebSocket -> 40台工位屏
                                      |                  |
                                      v                  v
                                  本地缓冲            Oracle/审计/报表
```

## 模块边界

1. C++ 采集服务只读访问 PLC TCP 102，不写 PLC。
2. C++ 输出标准 `StationEvent`，不做业务 CT 判定。
3. Java 消费事件，执行 CT 匹配、状态机、告警与推送。
4. Oracle 写入由 Java 数据服务统一处理，采集服务不直接大量写库。
