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
    /** 黄区起始：已用/标准 CT ≥ 该值（来自 T_CT_CONFIG.WARN_THRESHOLD，默认对齐 V1.2 50%） */
    public double warnThresholdElapsed = 0.5;
    /** 红区起始：已用/标准 CT ≥ 该值（T_CT_CONFIG.ALARM_THRESHOLD，默认 80%） */
    public double alarmThresholdElapsed = 0.8;
    public OffsetDateTime startTime;
    /** 已累计完成的暂停秒数（已结束的 hold 段） */
    public long holdSeconds = 0;
    /** 当前暂停段开始时间，用于冻结倒计时（V1.2 §3.4 HOLD） */
    public OffsetDateTime holdStartTime;
}
