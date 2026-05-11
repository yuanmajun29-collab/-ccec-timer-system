#include "EventPublisher.hpp"
#include <iostream>

EventPublisher::EventPublisher(std::string redisUrl)
    : redisUrl_(std::move(redisUrl)) {}

bool EventPublisher::publish(const StationEvent& event) {
    // TODO: 使用 hiredis/redis-plus-plus 写入 Redis Stream: station:event:queue
    // 失败时写入 data/cache/pending-events.log，恢复后补写。
    std::cout << "[PUBLISH] " << event.toJson() << std::endl;
    return true;
}
