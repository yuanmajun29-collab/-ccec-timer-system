package com.ccec.timer.domain;

import java.time.OffsetDateTime;

public record StationEvent(
        long eventSeq,
        String stationCode,
        String so,
        String esn,
        String engineType,
        boolean arriveFlag,
        boolean leaveFlag,
        boolean holdFlag,
        boolean reworkFlag,
        boolean bypassFlag,
        OffsetDateTime plcTimestamp
) {}
