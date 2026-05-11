package com.ccec.timer.domain;

import java.time.OffsetDateTime;

public record StationSnapshot(
        String stationCode,
        String so,
        String esn,
        String engineType,
        int standardCt,
        long elapsed,
        long remain,
        StationStatus status,
        String color,
        OffsetDateTime serverTime
) {}
