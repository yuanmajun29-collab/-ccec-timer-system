package com.ccec.timer.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlarmManagementService {
    private final JdbcTemplate jdbc;

    public AlarmManagementService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String handleStatus) {
        if (handleStatus != null && !handleStatus.isBlank()) {
            return jdbc.query(
                    """
                            SELECT ID, ALARM_NO, ALARM_TYPE, ALARM_LEVEL, STATION_CODE, ALARM_DESC,
                                   START_TIME, END_TIME, HANDLE_STATUS
                            FROM T_ALARM_RECORD WHERE HANDLE_STATUS = ? ORDER BY START_TIME DESC FETCH FIRST 500 ROWS ONLY
                            """,
                    (rs, row) -> row(rs),
                    handleStatus
            );
        }
        return jdbc.query(
                """
                        SELECT ID, ALARM_NO, ALARM_TYPE, ALARM_LEVEL, STATION_CODE, ALARM_DESC,
                               START_TIME, END_TIME, HANDLE_STATUS
                        FROM T_ALARM_RECORD ORDER BY START_TIME DESC FETCH FIRST 500 ROWS ONLY
                        """,
                (rs, row) -> row(rs)
        );
    }

    public boolean updateHandleStatus(long id, String handleStatus) {
        int n = jdbc.update(
                """
                        UPDATE T_ALARM_RECORD SET HANDLE_STATUS = ?, END_TIME = SYSDATE WHERE ID = ?
                        """,
                handleStatus,
                id
        );
        return n > 0;
    }

    private static Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("ID"));
        m.put("alarmNo", rs.getString("ALARM_NO"));
        m.put("alarmType", rs.getString("ALARM_TYPE"));
        m.put("alarmLevel", rs.getString("ALARM_LEVEL"));
        m.put("stationCode", rs.getString("STATION_CODE"));
        m.put("alarmDesc", rs.getString("ALARM_DESC"));
        Timestamp st = rs.getTimestamp("START_TIME");
        m.put("startTime", st != null ? st.toInstant().toString() : null);
        Timestamp et = rs.getTimestamp("END_TIME");
        m.put("endTime", et != null ? et.toInstant().toString() : null);
        m.put("handleStatus", rs.getString("HANDLE_STATUS"));
        return m;
    }
}
