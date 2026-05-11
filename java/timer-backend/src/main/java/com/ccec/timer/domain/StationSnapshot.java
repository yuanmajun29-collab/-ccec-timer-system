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
        OffsetDateTime serverTime,
        /** 离站时本周期实际秒数，其它时刻为 null */
        Long completedActualSeconds,
        /** 离站时本周期开始时间（用于写 T_PRODUCTION_RECORD），其它时刻为 null */
        OffsetDateTime cycleStartTime
) {}
