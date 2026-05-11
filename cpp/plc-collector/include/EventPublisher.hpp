#pragma once
#include "StationEvent.hpp"
#include <string>

class EventPublisher {
public:
    explicit EventPublisher(std::string redisUrl);
    bool publish(const StationEvent& event);

private:
    std::string redisUrl_;
};
