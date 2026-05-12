package com.ccec.timer.ws;

import com.ccec.timer.domain.StationSnapshot;
import com.ccec.timer.domain.StationStatus;

import java.time.format.DateTimeFormatter;

/**
 * 工位屏 WSS 载荷（V1.2 §3.8），与 Web 示例 JSON 字段一致，便于终端解析。
 */
public class StateUpdateMessage {
    public String type = "STATE_UPDATE";
    public String stationCode;
    public String state;
    public String soNo;
    public String esnNo;
    public String engineType;
    public int ct;
    public long elapsed;
    /** 剩余秒数；超时后为负数，屏端显示 +mm:ss 反向计时（V1.2 §3.5） */
    public long remain;
    public String color;
    public String serverTime;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static StateUpdateMessage fromSnapshot(StationSnapshot s) {
        StateUpdateMessage m = new StateUpdateMessage();
        m.stationCode = s.stationCode();
        m.state = wireState(s.status());
        m.soNo = s.so() == null ? "" : s.so();
        m.esnNo = s.esn() == null ? "" : s.esn();
        m.engineType = s.engineType() == null ? "" : s.engineType();
        m.ct = s.standardCt();
        m.elapsed = s.elapsed();
        m.remain = s.remain();
        m.color = s.color();
        m.serverTime = s.serverTime() == null ? "" : ISO.format(s.serverTime());
        return m;
    }

    /** 对外状态字：黄/红/超时仍归并为 RUNNING，与 V1.2 六状态表述一致 */
    static String wireState(StationStatus st) {
        return switch (st) {
            case WARN, ALARM, OVERTIME -> "RUNNING";
            default -> st.name();
        };
    }
}
