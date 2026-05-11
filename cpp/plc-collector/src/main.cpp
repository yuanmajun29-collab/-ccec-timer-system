#include "PlcClient.hpp"
#include "EventPublisher.hpp"
#include <chrono>
#include <cstdlib>
#include <iostream>
#include <string>
#include <thread>

static std::string envOrDefault(const char* name, const std::string& fallback) {
    const char* value = std::getenv(name);
    return value == nullptr || std::string(value).empty() ? fallback : std::string(value);
}

static int envIntOrDefault(const char* name, int fallback) {
    const char* value = std::getenv(name);
    if (value == nullptr) return fallback;
    try { return std::stoi(value); } catch (...) { return fallback; }
}

int main() {
    const std::string plcHost = envOrDefault("PLC_HOST", "128.10.40.10");
    const int plcRack = envIntOrDefault("PLC_RACK", 0);
    const int plcSlot = envIntOrDefault("PLC_SLOT", 1);
    const std::string redisUrl = envOrDefault("REDIS_URL", "redis://127.0.0.1:6379");
    const int pollIntervalMs = envIntOrDefault("POLL_INTERVAL_MS", 500);

    PlcClient plc(plcHost, plcRack, plcSlot);
    EventPublisher publisher(redisUrl);

    std::cout << "CCEC PLC collector starting. PLC=" << plcHost
              << " rack=" << plcRack << " slot=" << plcSlot
              << " redis=" << redisUrl << std::endl;

    while (!plc.connect()) {
        std::cerr << "PLC connect failed, retrying..." << std::endl;
        std::this_thread::sleep_for(std::chrono::seconds(5));
    }

    while (true) {
        if (!plc.healthy()) {
            plc.connect();
        }

        for (const auto& event : plc.pollMirrorDb()) {
            std::string reason;
            if (!event.isValid(reason)) {
                std::cerr << "[INVALID_EVENT] " << reason << std::endl;
                continue;
            }
            publisher.publish(event);
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(pollIntervalMs));
    }
}
