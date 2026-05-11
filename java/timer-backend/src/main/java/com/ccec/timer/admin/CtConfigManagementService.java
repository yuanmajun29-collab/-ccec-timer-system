package com.ccec.timer.admin;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CtConfigManagementService {
    private final JdbcTemplate jdbc;

    public CtConfigManagementService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listAll(String stationCodeFilter) {
        if (stationCodeFilter != null && !stationCodeFilter.isBlank()) {
            return jdbc.query(
                    """
                            SELECT ID, CONFIG_VERSION, ENGINE_TYPE, STATION_CODE, STANDARD_CT,
                                   WARN_THRESHOLD, ALARM_THRESHOLD, SOUND_POLICY, EFFECTIVE_TIME, ENABLED
                            FROM T_CT_CONFIG WHERE STATION_CODE = ? ORDER BY EFFECTIVE_TIME DESC
                            """,
                    (rs, row) -> row(rs),
                    stationCodeFilter
            );
        }
        return jdbc.query(
                """
                        SELECT ID, CONFIG_VERSION, ENGINE_TYPE, STATION_CODE, STANDARD_CT,
                               WARN_THRESHOLD, ALARM_THRESHOLD, SOUND_POLICY, EFFECTIVE_TIME, ENABLED
                        FROM T_CT_CONFIG ORDER BY STATION_CODE, EFFECTIVE_TIME DESC
                        """,
                (rs, row) -> row(rs)
        );
    }

    public Optional<Map<String, Object>> findById(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                            SELECT ID, CONFIG_VERSION, ENGINE_TYPE, STATION_CODE, STANDARD_CT,
                                   WARN_THRESHOLD, ALARM_THRESHOLD, SOUND_POLICY, EFFECTIVE_TIME, ENABLED
                            FROM T_CT_CONFIG WHERE ID = ?
                            """,
                    (rs, row) -> row(rs),
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public long create(Map<String, Object> body) {
        Long id = jdbc.queryForObject("SELECT SEQ_CT_ID.NEXTVAL FROM DUAL", Long.class);
        OffsetDateTime effective = OffsetDateTime.parse(String.valueOf(body.get("effectiveTime")));
        Date effectiveDate = Date.from(effective.atZoneSameInstant(ZoneId.systemDefault()).toInstant());
        jdbc.update(
                """
                        INSERT INTO T_CT_CONFIG (
                          ID, CONFIG_VERSION, ENGINE_TYPE, STATION_CODE, STANDARD_CT,
                          WARN_THRESHOLD, ALARM_THRESHOLD, SOUND_POLICY, EFFECTIVE_TIME, ENABLED
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                body.get("configVersion"),
                nullableString(body, "engineType"),
                body.get("stationCode"),
                intVal(body, "standardCt"),
                doubleVal(body, "warnThreshold"),
                doubleVal(body, "alarmThreshold"),
                nullableString(body, "soundPolicy"),
                effectiveDate,
                body.get("enabled") == null || Boolean.TRUE.equals(body.get("enabled")) ? 1 : 0
        );
        return id;
    }

    public boolean update(long id, Map<String, Object> body) {
        OffsetDateTime effective = OffsetDateTime.parse(String.valueOf(body.get("effectiveTime")));
        Date effectiveDate = Date.from(effective.atZoneSameInstant(ZoneId.systemDefault()).toInstant());
        int n = jdbc.update(
                """
                        UPDATE T_CT_CONFIG SET
                          CONFIG_VERSION = ?, ENGINE_TYPE = ?, STATION_CODE = ?, STANDARD_CT = ?,
                          WARN_THRESHOLD = ?, ALARM_THRESHOLD = ?, SOUND_POLICY = ?, EFFECTIVE_TIME = ?, ENABLED = ?
                        WHERE ID = ?
                        """,
                body.get("configVersion"),
                nullableString(body, "engineType"),
                body.get("stationCode"),
                intVal(body, "standardCt"),
                doubleVal(body, "warnThreshold"),
                doubleVal(body, "alarmThreshold"),
                nullableString(body, "soundPolicy"),
                effectiveDate,
                body.get("enabled") == null || Boolean.TRUE.equals(body.get("enabled")) ? 1 : 0,
                id
        );
        return n > 0;
    }

    private static String nullableString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static int intVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }

    private static double doubleVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v));
    }

    private static Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("ID"));
        m.put("configVersion", rs.getString("CONFIG_VERSION"));
        m.put("engineType", rs.getString("ENGINE_TYPE"));
        m.put("stationCode", rs.getString("STATION_CODE"));
        m.put("standardCt", rs.getInt("STANDARD_CT"));
        m.put("warnThreshold", rs.getDouble("WARN_THRESHOLD"));
        m.put("alarmThreshold", rs.getDouble("ALARM_THRESHOLD"));
        m.put("soundPolicy", rs.getString("SOUND_POLICY"));
        Timestamp ts = rs.getTimestamp("EFFECTIVE_TIME");
        m.put("effectiveTime", ts != null ? ts.toInstant().toString() : null);
        m.put("enabled", rs.getInt("ENABLED") == 1);
        return m;
    }
}
