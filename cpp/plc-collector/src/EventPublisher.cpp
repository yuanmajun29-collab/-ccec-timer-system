#include "EventPublisher.hpp"
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <hiredis/hiredis.h>
#include <iostream>

static std::string envOrDefault(const char* name, const std::string& fallback) {
    const char* value = std::getenv(name);
    return value == nullptr || std::string(value).empty() ? fallback : std::string(value);
}

bool EventPublisher::parseRedisUrl(const std::string& url, std::string& host, int& port) {
    const std::string prefix = "redis://";
    if (url.size() > prefix.size() && url.compare(0, prefix.size(), prefix) == 0) {
        std::string rest = url.substr(prefix.size());
        const auto colon = rest.find(':');
        if (colon != std::string::npos) {
            host = rest.substr(0, colon);
            try {
                port = std::stoi(rest.substr(colon + 1));
            } catch (...) {
                port = 6379;
            }
            return true;
        }
        host = rest;
        port = 6379;
        return true;
    }
    host = "127.0.0.1";
    port = 6379;
    return true;
}

EventPublisher::EventPublisher(std::string redisUrl)
    : redisUrl_(std::move(redisUrl)),
      streamKey_(envOrDefault("REDIS_STREAM_KEY", "station:event:queue")) {
    parseRedisUrl(redisUrl_, host_, port_);
}

EventPublisher::~EventPublisher() {
    disconnect();
}

void EventPublisher::disconnect() {
    if (ctx_) {
        redisFree(ctx_);
        ctx_ = nullptr;
    }
}

bool EventPublisher::ensureConnected() {
    if (ctx_ != nullptr && ctx_->err == 0) {
        return true;
    }
    disconnect();
    ctx_ = redisConnect(host_.c_str(), port_);
    if (ctx_ == nullptr || ctx_->err) {
        if (ctx_) {
            std::cerr << "[REDIS] connect error: " << ctx_->errstr << std::endl;
            redisFree(ctx_);
            ctx_ = nullptr;
        } else {
            std::cerr << "[REDIS] connect failed (alloc)" << std::endl;
        }
        return false;
    }
    const char* password = std::getenv("REDIS_PASSWORD");
    if (password != nullptr && std::string(password).length() > 0) {
        redisReply* authReply = (redisReply*)redisCommand(ctx_, "AUTH %s", password);
        if (authReply) {
            bool ok = authReply->type == REDIS_REPLY_STATUS && std::string(authReply->str) == "OK";
            freeReplyObject(authReply);
            if (!ok) {
                std::cerr << "[REDIS] AUTH failed" << std::endl;
                disconnect();
                return false;
            }
        }
    }
    return true;
}

void EventPublisher::appendPendingLog(const std::string& json) {
    std::error_code ec;
    std::filesystem::create_directories("data/cache", ec);
    std::ofstream out("data/cache/pending-events.log", std::ios::app);
    if (out) {
        out << json << '\n';
    }
}

bool EventPublisher::publish(const StationEvent& event) {
    const std::string json = event.toJson();
    if (!ensureConnected()) {
        appendPendingLog(json);
        return false;
    }
    redisReply* reply = (redisReply*)redisCommand(
            ctx_,
            "XADD %s * payload %s",
            streamKey_.c_str(),
            json.c_str());
    if (reply == nullptr || ctx_->err) {
        std::cerr << "[REDIS] XADD failed: " << (ctx_->err ? ctx_->errstr : "null reply") << std::endl;
        appendPendingLog(json);
        disconnect();
        if (reply) {
            freeReplyObject(reply);
        }
        return false;
    }
    freeReplyObject(reply);
    return true;
}
