#pragma once
#include <string>
#include <cstdint>

struct StationEvent {
    int64_t eventSeq{};
    std::string stationCode;
    std::string so;
    std::string esn;
    std::string engineType;
    bool arriveFlag{};
    bool leaveFlag{};
    bool holdFlag{};
    bool reworkFlag{};
    bool bypassFlag{};
    std::string plcTimestamp;

    bool isValid(std::string& reason) const;
    std::string toJson() const;
};
