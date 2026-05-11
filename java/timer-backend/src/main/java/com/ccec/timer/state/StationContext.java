package com.ccec.timer.state;

import com.ccec.timer.domain.StationStatus;
import java.time.OffsetDateTime;

public class StationContext {
    public StationStatus status = StationStatus.IDLE;
    public String stationCode;
    public String so;
    public String esn;
    public String engineType;
    public int standardCt = 300;
    public OffsetDateTime startTime;
    public long holdSeconds = 0;
}
