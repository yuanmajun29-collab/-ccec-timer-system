package com.ccec.timer.service;

@FunctionalInterface
public interface CtSecondsResolver {
    int resolveStandardCtSeconds(String engineType, String stationCode);
}
