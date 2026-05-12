package com.ccec.timer.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class JdbcCtConfigResolver implements CtConfigResolver {
    private static final String SELECT_ENGINE_STATION = """
            SELECT STANDARD_CT, WARN_THRESHOLD, ALARM_THRESHOLD FROM (
              SELECT STANDARD_CT, WARN_THRESHOLD, ALARM_THRESHOLD
              FROM T_CT_CONFIG
              WHERE ENABLED = 1 AND STATION_CODE = ? AND ENGINE_TYPE = ?
              ORDER BY EFFECTIVE_TIME DESC
            ) WHERE ROWNUM = 1
            """;
    private static final String SELECT_STATION_DEFAULT = """
            SELECT STANDARD_CT, WARN_THRESHOLD, ALARM_THRESHOLD FROM (
              SELECT STANDARD_CT, WARN_THRESHOLD, ALARM_THRESHOLD
              FROM T_CT_CONFIG
              WHERE ENABLED = 1 AND STATION_CODE = ? AND ENGINE_TYPE IS NULL
              ORDER BY EFFECTIVE_TIME DESC
            ) WHERE ROWNUM = 1
            """;

    private final JdbcTemplate jdbc;

    public JdbcCtConfigResolver(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CtConfig> resolve(String engineType, String stationCode) {
        if (stationCode == null || stationCode.isBlank()) {
            return Optional.empty();
        }
        if (engineType != null && !engineType.isBlank()) {
            Optional<CtConfig> row = queryRow(SELECT_ENGINE_STATION, stationCode, engineType);
            if (row.isPresent()) {
                return row;
            }
        }
        return queryRow(SELECT_STATION_DEFAULT, stationCode);
    }

    private Optional<CtConfig> queryRow(String sql, Object... args) {
        List<CtConfig> rows =
                jdbc.query(sql, (rs, rowNum) -> mapRow(rs), args);
        return rows.stream().filter(Objects::nonNull).findFirst();
    }

    private static CtConfig mapRow(ResultSet rs) throws SQLException {
        int ct = rs.getInt("STANDARD_CT");
        if (ct <= 0) {
            return null;
        }
        return new CtConfig(
                ct,
                toDouble(rs.getObject("WARN_THRESHOLD")),
                toDouble(rs.getObject("ALARM_THRESHOLD"))
        );
    }

    private static double toDouble(Object o) {
        if (o == null) {
            return 0.5;
        }
        if (o instanceof BigDecimal bd) {
            return bd.doubleValue();
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return 0.5;
    }
}
