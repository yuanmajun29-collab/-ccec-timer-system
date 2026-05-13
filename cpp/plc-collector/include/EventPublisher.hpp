#pragma once
#include "StationEvent.hpp"
#include <string>

struct redisContext;

class EventPublisher {
public:
    explicit EventPublisher(std::string redisUrl);
    ~EventPublisher();
    EventPublisher(const EventPublisher&) = delete;
    EventPublisher& operator=(const EventPublisher&) = delete;

    bool publish(const StationEvent& event);

private:
    std::string redisUrl_;
    std::string streamKey_;
    std::string pendingLogPath_;
    std::string host_;
    int port_{6379};
    redisContext* ctx_{nullptr};

    void disconnect();
    bool ensureConnected();
    void appendPendingLog(const std::string& json);
    bool flushPendingLog();
    bool publishJsonToRedis(const std::string& json);
    static bool parseRedisUrl(const std::string& url, std::string& host, int& port);
};
