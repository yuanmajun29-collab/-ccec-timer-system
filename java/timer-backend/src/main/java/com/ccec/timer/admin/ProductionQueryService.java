package com.ccec.timer.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductionQueryService {
    private final JdbcTemplate jdbc;

    public ProductionQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> page(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(200, Math.max(1, size));
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM T_PRODUCTION_RECORD", Long.class);
        int offset = p * s;
        List<Map<String, Object>> items = jdbc.query(
                """
                        SELECT ID, RECORD_NO, STATION_CODE, SO_NO, ESN_NO, ENGINE_TYPE,
                               STANDARD_CT, ACTUAL_CT, START_TIME, END_TIME, STATUS
                        FROM T_PRODUCTION_RECORD ORDER BY START_TIME DESC
                        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                        """,
                (rs, row) -> row(rs),
                offset,
                s
        );
        return Map.of(
                "total", total,
                "page", p,
                "size", s,
                "items", items
        );
    }

    private static Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("ID"));
        m.put("recordNo", rs.getString("RECORD_NO"));
        m.put("stationCode", rs.getString("STATION_CODE"));
        m.put("soNo", rs.getString("SO_NO"));
        m.put("esnNo", rs.getString("ESN_NO"));
        m.put("engineType", rs.getString("ENGINE_TYPE"));
        m.put("standardCt", rs.getInt("STANDARD_CT"));
        Object act = rs.getObject("ACTUAL_CT");
        m.put("actualCt", act != null ? rs.getLong("ACTUAL_CT") : null);
        Timestamp st = rs.getTimestamp("START_TIME");
        m.put("startTime", st != null ? st.toInstant().toString() : null);
        Timestamp et = rs.getTimestamp("END_TIME");
        m.put("endTime", et != null ? et.toInstant().toString() : null);
        m.put("status", rs.getString("STATUS"));
        return m;
    }
}
