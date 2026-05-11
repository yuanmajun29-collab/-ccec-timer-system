package com.ccec.timer.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditQueryService {
    private final JdbcTemplate jdbc;

    public AuditQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> page(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(200, Math.max(1, size));
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM T_AUDIT_LOG", Long.class);
        int offset = p * s;
        List<Map<String, Object>> items = jdbc.query(
                """
                        SELECT ID, USER_ID, USER_NAME, OPERATION_TYPE, OBJECT_TYPE, OBJECT_ID, CREATED_AT
                        FROM T_AUDIT_LOG ORDER BY CREATED_AT DESC
                        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                        """,
                (rs, row) -> row(rs),
                offset,
                s
        );
        return Map.of("total", total, "page", p, "size", s, "items", items);
    }

    private static Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("ID"));
        m.put("userId", rs.getString("USER_ID"));
        m.put("userName", rs.getString("USER_NAME"));
        m.put("operationType", rs.getString("OPERATION_TYPE"));
        m.put("objectType", rs.getString("OBJECT_TYPE"));
        m.put("objectId", rs.getString("OBJECT_ID"));
        Timestamp ts = rs.getTimestamp("CREATED_AT");
        m.put("createdAt", ts != null ? ts.toInstant().toString() : null);
        return m;
    }
}
