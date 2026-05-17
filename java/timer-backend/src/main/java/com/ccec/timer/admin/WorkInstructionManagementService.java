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
public class WorkInstructionManagementService {
    private final JdbcTemplate jdbc;

    public WorkInstructionManagementService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listAll(String stationCodeFilter) {
        if (stationCodeFilter != null && !stationCodeFilter.isBlank()) {
            return jdbc.query(
                    """
                            SELECT ID, DOC_NO, TITLE, STATION_CODE, ENGINE_TYPE, VERSION_NO,
                                   CONTENT_TEXT, SAFETY_NOTES, ENABLED, CREATED_AT, UPDATED_AT
                            FROM T_WORK_INSTRUCTION
                            WHERE STATION_CODE = ?
                            ORDER BY UPDATED_AT DESC, ID DESC
                            """,
                    (rs, rowNum) -> row(rs),
                    stationCodeFilter
            );
        }
        return jdbc.query(
                """
                        SELECT ID, DOC_NO, TITLE, STATION_CODE, ENGINE_TYPE, VERSION_NO,
                               CONTENT_TEXT, SAFETY_NOTES, ENABLED, CREATED_AT, UPDATED_AT
                        FROM T_WORK_INSTRUCTION
                        ORDER BY STATION_CODE, UPDATED_AT DESC, ID DESC
                        """,
                (rs, rowNum) -> row(rs)
        );
    }

    public Optional<Map<String, Object>> findById(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                            SELECT ID, DOC_NO, TITLE, STATION_CODE, ENGINE_TYPE, VERSION_NO,
                                   CONTENT_TEXT, SAFETY_NOTES, ENABLED, CREATED_AT, UPDATED_AT
                            FROM T_WORK_INSTRUCTION
                            WHERE ID = ?
                            """,
                    (rs, rowNum) -> row(rs),
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public long create(Map<String, Object> body) {
        Long id = jdbc.queryForObject("SELECT SEQ_WORK_INSTRUCTION_ID.NEXTVAL FROM DUAL", Long.class);
        jdbc.update(
                """
                        INSERT INTO T_WORK_INSTRUCTION (
                          ID, DOC_NO, TITLE, STATION_CODE, ENGINE_TYPE, VERSION_NO,
                          CONTENT_TEXT, SAFETY_NOTES, ENABLED, CREATED_AT, UPDATED_AT
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE)
                        """,
                id,
                body.get("docNo"),
                body.get("title"),
                body.get("stationCode"),
                nullableString(body, "engineType"),
                body.get("versionNo"),
                body.get("contentText"),
                nullableString(body, "safetyNotes"),
                enabledFlag(body)
        );
        return id;
    }

    public boolean update(long id, Map<String, Object> body) {
        int n = jdbc.update(
                """
                        UPDATE T_WORK_INSTRUCTION SET
                          DOC_NO = ?, TITLE = ?, STATION_CODE = ?, ENGINE_TYPE = ?, VERSION_NO = ?,
                          CONTENT_TEXT = ?, SAFETY_NOTES = ?, ENABLED = ?, UPDATED_AT = SYSDATE
                        WHERE ID = ?
                        """,
                body.get("docNo"),
                body.get("title"),
                body.get("stationCode"),
                nullableString(body, "engineType"),
                body.get("versionNo"),
                body.get("contentText"),
                nullableString(body, "safetyNotes"),
                enabledFlag(body),
                id
        );
        return n > 0;
    }

    public Optional<Map<String, Object>> findLatestEnabledByStation(String stationCode) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    """
                            SELECT * FROM (
                              SELECT ID, DOC_NO, TITLE, STATION_CODE, ENGINE_TYPE, VERSION_NO,
                                     CONTENT_TEXT, SAFETY_NOTES, ENABLED, CREATED_AT, UPDATED_AT
                              FROM T_WORK_INSTRUCTION
                              WHERE STATION_CODE = ? AND ENABLED = 1
                              ORDER BY UPDATED_AT DESC, ID DESC
                            ) WHERE ROWNUM = 1
                            """,
                    (rs, rowNum) -> row(rs),
                    stationCode
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private static int enabledFlag(Map<String, Object> body) {
        return body.get("enabled") == null || Boolean.TRUE.equals(body.get("enabled")) ? 1 : 0;
    }

    private static String nullableString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("ID"));
        m.put("docNo", rs.getString("DOC_NO"));
        m.put("title", rs.getString("TITLE"));
        m.put("stationCode", rs.getString("STATION_CODE"));
        m.put("engineType", rs.getString("ENGINE_TYPE"));
        m.put("versionNo", rs.getString("VERSION_NO"));
        m.put("contentText", rs.getString("CONTENT_TEXT"));
        m.put("safetyNotes", rs.getString("SAFETY_NOTES"));
        m.put("enabled", rs.getInt("ENABLED") == 1);
        Timestamp createdAt = rs.getTimestamp("CREATED_AT");
        Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
        m.put("createdAt", createdAt != null ? createdAt.toInstant().toString() : null);
        m.put("updatedAt", updatedAt != null ? updatedAt.toInstant().toString() : null);
        return m;
    }
}
