package com.ccec.timer.state;

import com.ccec.timer.domain.StationEvent;
import com.ccec.timer.domain.StationSnapshot;
import com.ccec.timer.domain.StationStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StationStateMachine {
    private final Map<String, StationContext> contexts = new ConcurrentHashMap<>();

    public StationSnapshot apply(StationEvent event) {
        StationContext ctx = contexts.computeIfAbsent(event.stationCode(), code -> {
            StationContext c = new StationContext();
            c.stationCode = code;
            return c;
        });

        OffsetDateTime now = OffsetDateTime.now();

        if (event.arriveFlag() && ctx.status == StationStatus.IDLE) {
            ctx.status = StationStatus.RUNNING;
            ctx.so = event.so();
            ctx.esn = event.esn();
            ctx.engineType = event.engineType();
            ctx.startTime = now;
            ctx.standardCt = resolveCt(event.engineType(), event.stationCode());
        } else if (event.leaveFlag()) {
            ctx.status = StationStatus.IDLE;
            ctx.startTime = null;
        } else if (event.holdFlag() && ctx.status != StationStatus.IDLE) {
            ctx.status = StationStatus.HOLD;
        } else if (event.reworkFlag()) {
            ctx.status = StationStatus.REWORK;
        } else if (event.bypassFlag()) {
            ctx.status = StationStatus.BYPASS;
        }

        return snapshot(ctx, now);
    }

    private StationSnapshot snapshot(StationContext ctx, OffsetDateTime now) {
        long elapsed = ctx.startTime == null ? 0 : Math.max(0, Duration.between(ctx.startTime, now).toSeconds() - ctx.holdSeconds);
        if (ctx.status == StationStatus.RUNNING || ctx.status == StationStatus.WARN || ctx.status == StationStatus.ALARM) {
            double ratio = ctx.standardCt == 0 ? 0 : (double) elapsed / ctx.standardCt;
            if (ratio > 1.0) ctx.status = StationStatus.OVERTIME;
            else if (ratio > 0.9) ctx.status = StationStatus.ALARM;
            else if (ratio > 0.7) ctx.status = StationStatus.WARN;
            else ctx.status = StationStatus.RUNNING;
        }
        long remain = ctx.standardCt - elapsed;
        return new StationSnapshot(ctx.stationCode, ctx.so, ctx.esn, ctx.engineType, ctx.standardCt, elapsed, remain, ctx.status, colorOf(ctx.status), now);
    }

    private int resolveCt(String engineType, String stationCode) {
        // TODO: 优先查询 T_CT_CONFIG：机型+工位 > 工位默认 > 系统默认。
        return 300;
    }

    private String colorOf(StationStatus status) {
        return switch (status) {
            case RUNNING -> "GREEN";
            case WARN -> "YELLOW";
            case ALARM, OVERTIME -> "RED";
            case HOLD -> "BLUE";
            case REWORK -> "PURPLE";
            default -> "GRAY";
        };
    }
}
