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

find_path(HIREDIS_INCLUDE_DIR NAMES hiredis/hiredis.h)
find_library(HIREDIS_LIBRARY NAMES hiredis)
if(NOT HIREDIS_INCLUDE_DIR OR NOT HIREDIS_LIBRARY)
  message(FATAL_ERROR "hiredis not found (install libhiredis-dev or hiredis)")
endif()

target_include_directories(plc_collector PRIVATE include ${HIREDIS_INCLUDE_DIR})
target_link_libraries(plc_collector PRIVATE ${HIREDIS_LIBRARY})
