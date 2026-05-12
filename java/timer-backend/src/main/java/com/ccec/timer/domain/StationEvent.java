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
        /** PLC 异常代码，0 或 null 表示无异常（DB900.abnormal_code，V1.2 §3.2） */
        Integer abnormalCode,
        OffsetDateTime plcTimestamp
) {}
