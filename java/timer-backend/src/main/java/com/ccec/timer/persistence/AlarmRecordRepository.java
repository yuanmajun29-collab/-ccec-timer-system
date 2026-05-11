package com.ccec.timer.persistence;

import com.ccec.timer.domain.StationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public class AlarmRecordRepository {
    private final JdbcTemplate jdbc;

    public AlarmRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertCtExceeded(String stationCode, StationStatus level, String description) {
        Long id = jdbc.queryForObject("SELECT SEQ_ALARM_ID.NEXTVAL FROM DUAL", Long.class);
        String alarmNo = "AL-" + id;
        String alarmLevel = level == StationStatus.OVERTIME ? "CRITICAL" : "MAJOR";
        jdbc.update(
                """
                        INSERT INTO T_ALARM_RECORD (
                          ID, ALARM_NO, ALARM_TYPE, ALARM_LEVEL, STATION_CODE, ALARM_DESC, START_TIME, HANDLE_STATUS
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, alarmNo, "CT_EXCEEDED", alarmLevel, stationCode, description, new Date(), "OPEN"
        );
    }
}
