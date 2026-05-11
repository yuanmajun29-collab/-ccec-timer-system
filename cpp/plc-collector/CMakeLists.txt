cmake_minimum_required(VERSION 3.20)
project(ccec_plc_collector LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(plc_collector
    src/main.cpp
    src/StationEvent.cpp
    src/PlcClient.cpp
    src/EventPublisher.cpp
)

target_include_directories(plc_collector PRIVATE include)
