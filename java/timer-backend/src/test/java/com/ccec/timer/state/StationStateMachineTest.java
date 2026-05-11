package com.ccec.timer.state;

import com.ccec.timer.domain.StationEvent;
import com.ccec.timer.domain.StationStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationStateMachineTest {
    @Test
    void arriveStartsRunning() {
        StationStateMachine sm = new StationStateMachine();
        StationEvent event = new StationEvent(1, "A601", "SO1", "ESN1", "QSK60",
                true, false, false, false, false, OffsetDateTime.now());
        assertEquals(StationStatus.RUNNING, sm.apply(event).status());
    }

    @Test
    void leaveReturnsIdle() {
        StationStateMachine sm = new StationStateMachine();
        sm.apply(new StationEvent(1, "A601", "SO1", "ESN1", "QSK60", true, false, false, false, false, OffsetDateTime.now()));
        assertEquals(StationStatus.IDLE,
                sm.apply(new StationEvent(2, "A601", "SO1", "ESN1", "QSK60", false, true, false, false, false, OffsetDateTime.now())).status());
    }
}
