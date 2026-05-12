package com.ccec.timer.state;

import com.ccec.timer.domain.StationEvent;
import com.ccec.timer.domain.StationStatus;
import com.ccec.timer.service.CtConfig;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StationStateMachineTest {
    private static final OffsetDateTime TS = OffsetDateTime.parse("2026-05-12T10:00:00+08:00");

    @Test
    void arriveStartsRunning() {
        StationStateMachine sm =
                new StationStateMachine((engine, station) -> Optional.of(new CtConfig(300, 0.5, 0.8)));
        StationEvent event = new StationEvent(1, "A601", "SO1", "ESN1", "QSK60",
                true, false, false, false, false, null, TS);
        assertEquals(StationStatus.RUNNING, sm.apply(event).status());
    }

    @Test
    void leaveReturnsIdle() {
        StationStateMachine sm =
                new StationStateMachine((engine, station) -> Optional.of(new CtConfig(300, 0.5, 0.8)));
        sm.apply(new StationEvent(1, "A601", "SO1", "ESN1", "QSK60", true, false, false, false, false, null, TS));
        assertEquals(StationStatus.IDLE,
                sm.apply(new StationEvent(2, "A601", "SO1", "ESN1", "QSK60", false, true, false, false, false, null, TS)).status());
    }

    @Test
    void leaveRecordsCompletedSeconds() {
        StationStateMachine sm =
                new StationStateMachine((engine, station) -> Optional.of(new CtConfig(100, 0.5, 0.8)));
        sm.apply(new StationEvent(1, "A601", "SO1", "ESN1", "QSK60",
                true, false, false, false, false, null, TS));
        var snap = sm.apply(new StationEvent(2, "A601", "SO1", "ESN1", "QSK60",
                false, true, false, false, false, null, TS));
        assertEquals(StationStatus.IDLE, snap.status());
        assertNotNull(snap.completedActualSeconds());
        assertNotNull(snap.cycleStartTime());
    }

    @Test
    void unknownCtGoesAbnormal() {
        StationStateMachine sm = new StationStateMachine((engine, station) -> Optional.empty());
        StationEvent event = new StationEvent(1, "A601", "SO1", "ESN1", "UNKNOWN",
                true, false, false, false, false, null, TS);
        assertEquals(StationStatus.ABNORMAL, sm.apply(event).status());
    }

    @Test
    void abnormalCodeForcesAbnormal() {
        StationStateMachine sm =
                new StationStateMachine((engine, station) -> Optional.of(new CtConfig(300, 0.5, 0.8)));
        StationEvent event = new StationEvent(1, "A601", "SO1", "ESN1", "QSK60",
                true, false, false, false, false, 99, TS);
        assertEquals(StationStatus.ABNORMAL, sm.apply(event).status());
    }
}
