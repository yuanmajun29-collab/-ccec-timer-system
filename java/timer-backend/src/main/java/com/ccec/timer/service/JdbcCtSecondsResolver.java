package com.ccec.timer.service;

import com.ccec.timer.config.TimerProperties;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcCtSecondsResolver implements CtSecondsResolver {
    private static final String SELECT_ENGINE_STATION = """
            SELECT STANDARD_CT FROM (
              SELECT STANDARD_CT FROM T_CT_CONFIG
              WHERE ENABLED = 1 AND STATION_CODE = ? AND ENGINE_TYPE = ?
              ORDER BY EFFECTIVE_TIME DESC
            ) WHERE ROWNUM = 1
            """;
    private static final String SELECT_STATION_DEFAULT = """
            SELECT STANDARD_CT FROM (
              SELECT STANDARD_CT FROM T_CT_CONFIG
              WHERE ENABLED = 1 AND STATION_CODE = ? AND ENGINE_TYPE IS NULL
              ORDER BY EFFECTIVE_TIME DESC
            ) WHERE ROWNUM = 1
            """;

    private final JdbcTemplate jdbc;
    private final TimerProperties timerProperties;

    public JdbcCtSecondsResolver(JdbcTemplate jdbc, TimerProperties timerProperties) {
        this.jdbc = jdbc;
        this.timerProperties = timerProperties;
    }

    @Override
    public int resolveStandardCtSeconds(String engineType, String stationCode) {
        if (stationCode == null || stationCode.isBlank()) {
            return timerProperties.getCt().getDefaultSeconds();
        }
        if (engineType != null && !engineType.isBlank()) {
            Integer ct = queryOne(SELECT_ENGINE_STATION, stationCode, engineType);
            if (ct != null) {
                return ct;
            }
        }
        Integer stationCt = queryOne(SELECT_STATION_DEFAULT, stationCode);
        if (stationCt != null) {
            return stationCt;
        }
        return timerProperties.getCt().getDefaultSeconds();
    }

    private Integer queryOne(String sql, Object... args) {
        try {
            return jdbc.queryForObject(sql, Integer.class, args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
