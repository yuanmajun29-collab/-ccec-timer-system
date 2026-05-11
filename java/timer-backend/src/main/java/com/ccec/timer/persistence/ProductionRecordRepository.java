package com.ccec.timer.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

@Repository
public class ProductionRecordRepository {
    private final JdbcTemplate jdbc;

    public ProductionRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertCompletedCycle(
            String stationCode,
            String so,
            String esn,
            String engineType,
            int standardCt,
            long actualSeconds,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) {
        Long id = jdbc.queryForObject("SELECT SEQ_PRODUCTION_ID.NEXTVAL FROM DUAL", Long.class);
        String recordNo = "PR-" + id;
        Date start = toDate(startTime);
        Date end = toDate(endTime);
        jdbc.update(
                """
                        INSERT INTO T_PRODUCTION_RECORD (
                          ID, RECORD_NO, STATION_CODE, SO_NO, ESN_NO, ENGINE_TYPE,
                          STANDARD_CT, ACTUAL_CT, START_TIME, END_TIME, STATUS
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, recordNo, stationCode, so, esn, engineType,
                standardCt, actualSeconds, start, end, "COMPLETED"
        );
    }

    private static Date toDate(OffsetDateTime t) {
        if (t == null) {
            return null;
        }
        return Date.from(t.atZoneSameInstant(ZoneId.systemDefault()).toInstant());
    }
}
