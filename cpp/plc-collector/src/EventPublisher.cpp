#include "EventPublisher.hpp"
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <hiredis/hiredis.h>
#include <iostream>
#include <vector>

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
      streamKey_(envOrDefault("REDIS_STREAM_KEY", "station:event:queue")),
      pendingLogPath_(envOrDefault("PENDING_EVENT_LOG", "data/cache/pending-events.log")) {
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
    const auto parent = std::filesystem::path(pendingLogPath_).parent_path();
    if (!parent.empty()) {
        std::filesystem::create_directories(parent, ec);
    }
    std::ofstream out(pendingLogPath_, std::ios::app);
    if (out) {
        out << json << '\n';
    }
}

bool EventPublisher::publishJsonToRedis(const std::string& json) {
    redisReply* reply = (redisReply*)redisCommand(
            ctx_,
            "XADD %s * payload %s",
            streamKey_.c_str(),
            json.c_str());
    if (reply == nullptr || ctx_->err) {
        std::cerr << "[REDIS] XADD failed: " << (ctx_->err ? ctx_->errstr : "null reply") << std::endl;
        if (reply) {
            freeReplyObject(reply);
        }
        disconnect();
        return false;
    }
    freeReplyObject(reply);
    return true;
}

bool EventPublisher::flushPendingLog() {
    const std::string replayPath = pendingLogPath_ + ".replay";
    std::error_code ec;
    const bool hasPending = std::filesystem::exists(pendingLogPath_);
    const bool hasReplay = std::filesystem::exists(replayPath);
    if (!hasPending && !hasReplay) {
        return true;
    }

    if (hasPending) {
        if (hasReplay) {
            std::cerr << "[PENDING] replay file already exists, keeping pending log intact" << std::endl;
            return false;
        }
        std::filesystem::rename(pendingLogPath_, replayPath, ec);
        if (ec) {
            std::cerr << "[PENDING] cannot rotate pending log: " << ec.message() << std::endl;
            return false;
        }
    }

    std::ifstream in(replayPath);
    if (!in) {
        std::cerr << "[PENDING] cannot read rotated pending log" << std::endl;
        if (!std::filesystem::exists(pendingLogPath_)) {
            std::filesystem::rename(replayPath, pendingLogPath_, ec);
        }
        return false;
    }

    std::vector<std::string> lines;
    std::string line;
    while (std::getline(in, line)) {
        if (!line.empty()) {
            lines.push_back(line);
        }
    }
    in.close();

    for (std::size_t i = 0; i < lines.size(); ++i) {
        if (!publishJsonToRedis(lines[i])) {
            for (std::size_t j = i; j < lines.size(); ++j) {
                appendPendingLog(lines[j]);
            }
            std::filesystem::remove(replayPath, ec);
            std::cerr << "[PENDING] replay interrupted, remaining events kept" << std::endl;
            return false;
        }
    }

    std::filesystem::remove(replayPath, ec);
    if (!lines.empty()) {
        std::cout << "[PENDING] replayed " << lines.size() << " event(s)" << std::endl;
    }
    return true;
}

bool EventPublisher::publish(const StationEvent& event) {
    const std::string json = event.toJson();
    if (!ensureConnected()) {
        appendPendingLog(json);
        return false;
    }
    if (!flushPendingLog()) {
        appendPendingLog(json);
        return false;
    }
    if (!publishJsonToRedis(json)) {
        appendPendingLog(json);
        return false;
    }
    return true;
}
