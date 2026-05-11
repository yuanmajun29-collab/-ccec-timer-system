package com.ccec.timer.api;

import com.ccec.timer.config.TimerProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final TimerProperties timerProperties;

    public StationController(JdbcTemplate jdbc, StringRedisTemplate redis, TimerProperties timerProperties) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.timerProperties = timerProperties;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> items = jdbc.query(
                """
                        SELECT STATION_CODE AS stationCode, STATION_NAME AS stationName,
                               LINE_CODE AS lineCode, ENABLED AS enabled
                        FROM T_STATION
                        WHERE ENABLED = 1
                        ORDER BY STATION_CODE
                        """,
                (rs, rowNum) -> Map.of(
                        "stationCode", rs.getString("stationCode"),
                        "stationName", rs.getString("stationName"),
                        "lineCode", rs.getString("lineCode"),
                        "enabled", rs.getInt("enabled") == 1
                )
        );
        return Map.of("success", true, "code", "0", "message", "OK", "data", Map.of("items", items));
    }

    /**
     * 读取 Redis 中缓存的工位实时快照 JSON，无缓存时 data 为空对象。
     */
    @GetMapping("/{stationCode}/status-cache")
    public Map<String, Object> statusCache(@PathVariable String stationCode) {
        String key = timerProperties.getRedis().getStatusKeyPrefix() + stationCode;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return Map.of("success", true, "code", "0", "message", "OK", "data", Map.of());
        }
        return Map.of("success", true, "code", "0", "message", "OK", "data", Map.of("snapshotJson", json));
    }
}
