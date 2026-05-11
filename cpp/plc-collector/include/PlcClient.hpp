#pragma once
#include "StationEvent.hpp"
#include <string>
#include <vector>

class PlcClient {
public:
    PlcClient(std::string host, int rack, int slot);
    bool connect();
    bool healthy() const;
    std::vector<StationEvent> pollMirrorDb();

private:
    std::string host_;
    int rack_;
    int slot_;
    bool connected_{false};
    int64_t mockSeq_{0};
};
