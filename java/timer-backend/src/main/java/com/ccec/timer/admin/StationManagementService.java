package com.ccec.timer.admin;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StationManagementService {
    private final JdbcTemplate jdbc;

    public StationManagementService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.query(
                """
                        SELECT ID, STATION_CODE, STATION_NAME, LINE_CODE, SCREEN_CODE, SCREEN_IP, PLC_CODE, ENABLED, CREATED_AT
                        FROM T_STATION ORDER BY STATION_CODE
                        """,
                (rs, row) -> row(rs)
        );
    }

    public Optional<Map<String, Object>> findById(long id) {
        try {
            Map<String, Object> row = jdbc.queryForObject(
                    """
                            SELECT ID, STATION_CODE, STATION_NAME, LINE_CODE, SCREEN_CODE, SCREEN_IP, PLC_CODE, ENABLED, CREATED_AT
                            FROM T_STATION WHERE ID = ?
                            """,
                    (rs, row) -> row(rs),
                    id
            );
            return Optional.ofNullable(row);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public long create(Map<String, Object> body) {
        Long id = jdbc.queryForObject("SELECT SEQ_STATION_ID.NEXTVAL FROM DUAL", Long.class);
        jdbc.update(
                """
                        INSERT INTO T_STATION (
                          ID, STATION_CODE, STATION_NAME, LINE_CODE, SCREEN_CODE, SCREEN_IP, PLC_CODE, ENABLED
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                body.get("stationCode"),
                body.get("stationName"),
                body.get("lineCode"),
                body.get("screenCode"),
                body.get("screenIp"),
                body.get("plcCode"),
                enabledFlag(body)
        );
        return id;
    }

    public boolean update(long id, Map<String, Object> body) {
        int n = jdbc.update(
                """
                        UPDATE T_STATION SET
                          STATION_CODE = ?, STATION_NAME = ?, LINE_CODE = ?, SCREEN_CODE = ?, SCREEN_IP = ?, PLC_CODE = ?, ENABLED = ?
                        WHERE ID = ?
                        """,
                body.get("stationCode"),
                body.get("stationName"),
                body.get("lineCode"),
                body.get("screenCode"),
                body.get("screenIp"),
                body.get("plcCode"),
                enabledFlag(body),
                id
        );
        return n > 0;
    }

    private static int enabledFlag(Map<String, Object> body) {
        return body.get("enabled") == null || Boolean.TRUE.equals(body.get("enabled")) ? 1 : 0;
    }

    private static Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("ID"));
        m.put("stationCode", rs.getString("STATION_CODE"));
        m.put("stationName", rs.getString("STATION_NAME"));
        m.put("lineCode", rs.getString("LINE_CODE"));
        m.put("screenCode", rs.getString("SCREEN_CODE"));
        m.put("screenIp", rs.getString("SCREEN_IP"));
        m.put("plcCode", rs.getString("PLC_CODE"));
        m.put("enabled", rs.getInt("ENABLED") == 1);
        Timestamp ts = rs.getTimestamp("CREATED_AT");
        m.put("createdAt", ts != null ? ts.toInstant().toString() : null);
        return m;
    }
}
