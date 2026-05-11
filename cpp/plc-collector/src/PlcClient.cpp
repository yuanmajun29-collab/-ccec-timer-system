#include "PlcClient.hpp"
#include <chrono>
#include <ctime>

PlcClient::PlcClient(std::string host, int rack, int slot)
    : host_(std::move(host)), rack_(rack), slot_(slot) {}

bool PlcClient::connect() {
    // TODO: 接入 Snap7/厂商 S7 TCP SDK。必须保持只读，不调用写 DB/写变量接口。
    connected_ = true;
    return connected_;
}

bool PlcClient::healthy() const {
    return connected_;
}

std::vector<StationEvent> PlcClient::pollMirrorDb() {
    // TODO: 按 PLC-IF-V1.0 镜像 DB 偏移解析字段。
    // 当前为联调前的模拟事件，便于 Java 后端压测。
    StationEvent e;
    e.eventSeq = ++mockSeq_;
    e.stationCode = "A601";
    e.so = "SO20260511001";
    e.esn = "ESN000123456";
    e.engineType = "QSK60";
    e.arriveFlag = (mockSeq_ == 1);
    e.leaveFlag = (mockSeq_ == 20);
    e.plcTimestamp = "2026-05-11T14:30:15+08:00";
    return {e};
}
