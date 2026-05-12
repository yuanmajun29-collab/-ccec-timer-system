#include "StationEvent.hpp"
#include <sstream>

bool StationEvent::isValid(std::string& reason) const {
    if (eventSeq <= 0) { reason = "EventSeq must be positive"; return false; }
    if (stationCode.empty()) { reason = "StationCode is empty"; return false; }
    if (arriveFlag && (so.empty() || esn.empty())) {
        reason = "SO/ESN is required when ArriveFlag is true";
        return false;
    }
    return true;
}

std::string StationEvent::toJson() const {
    std::ostringstream os;
    os << "{"
       << "\"eventSeq\":" << eventSeq << ","
       << "\"stationCode\":\"" << stationCode << "\","
       << "\"so\":\"" << so << "\","
       << "\"esn\":\"" << esn << "\","
       << "\"engineType\":\"" << engineType << "\","
       << "\"arriveFlag\":" << (arriveFlag ? "true" : "false") << ","
       << "\"leaveFlag\":" << (leaveFlag ? "true" : "false") << ","
       << "\"holdFlag\":" << (holdFlag ? "true" : "false") << ","
       << "\"reworkFlag\":" << (reworkFlag ? "true" : "false") << ","
       << "\"bypassFlag\":" << (bypassFlag ? "true" : "false") << ","
       << "\"abnormalCode\":" << abnormalCode << ","
       << "\"plcTimestamp\":\"" << plcTimestamp << "\""
       << "}";
    return os.str();
}
