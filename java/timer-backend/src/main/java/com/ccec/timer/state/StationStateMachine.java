package com.ccec.timer.state;

import com.ccec.timer.domain.StationEvent;
import com.ccec.timer.domain.StationSnapshot;
import com.ccec.timer.domain.StationStatus;
import com.ccec.timer.service.CtConfig;
import com.ccec.timer.service.CtConfigResolver;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StationStateMachine {
    private final Map<String, StationContext> contexts = new ConcurrentHashMap<>();
    private final CtConfigResolver ctConfigResolver;

    public StationStateMachine(CtConfigResolver ctConfigResolver) {
        this.ctConfigResolver = ctConfigResolver;
    }

    public StationSnapshot apply(StationEvent event) {
        StationContext ctx = contexts.computeIfAbsent(event.stationCode(), code -> {
            StationContext c = new StationContext();
            c.stationCode = code;
            return c;
        });

        OffsetDateTime now = OffsetDateTime.now();
        Long completedActual = null;
        OffsetDateTime cycleStartForLog = null;

        if (ctx.status == StationStatus.ABNORMAL
                && (event.abnormalCode() == null || event.abnormalCode() == 0)) {
            ctx.status = StationStatus.IDLE;
        }

        if (event.abnormalCode() != null && event.abnormalCode() != 0) {
            ctx.status = StationStatus.ABNORMAL;
            ctx.startTime = null;
            ctx.holdStartTime = null;
            ctx.so = event.so();
            ctx.esn = event.esn();
            ctx.engineType = event.engineType();
            return snapshot(ctx, now, null, null);
        }

        if (event.leaveFlag()) {
            if (ctx.status == StationStatus.HOLD && ctx.holdStartTime != null) {
                ctx.holdSeconds += Duration.between(ctx.holdStartTime, now).getSeconds();
                ctx.holdStartTime = null;
            }
            if (ctx.status != StationStatus.BYPASS && ctx.startTime != null) {
                long el = effectiveElapsed(ctx, now);
                completedActual = Math.max(0, el);
                cycleStartForLog = ctx.startTime;
            }
            ctx.status = StationStatus.IDLE;
            ctx.startTime = null;
            ctx.holdSeconds = 0;
        } else if (event.arriveFlag()
                && (ctx.status == StationStatus.IDLE
                || ctx.status == StationStatus.ABNORMAL
                || ctx.status == StationStatus.BYPASS)) {
            Optional<CtConfig> cfg = ctConfigResolver.resolve(event.engineType(), event.stationCode());
            if (cfg.isEmpty()) {
                ctx.status = StationStatus.ABNORMAL;
                ctx.so = event.so();
                ctx.esn = event.esn();
                ctx.engineType = event.engineType();
                ctx.startTime = null;
                ctx.standardCt = 0;
                ctx.holdStartTime = null;
                return snapshot(ctx, now, null, null);
            }
            CtConfig c = cfg.get();
            ctx.status = StationStatus.RUNNING;
            ctx.so = event.so();
            ctx.esn = event.esn();
            ctx.engineType = event.engineType();
            ctx.startTime = now;
            ctx.standardCt = c.standardCtSeconds();
            ctx.warnThresholdElapsed = c.warnThresholdElapsed();
            ctx.alarmThresholdElapsed = c.alarmThresholdElapsed();
            ctx.holdSeconds = 0;
            ctx.holdStartTime = null;
        } else if (event.holdFlag() && ctx.status != StationStatus.IDLE) {
            if (ctx.status != StationStatus.HOLD && ctx.startTime != null) {
                ctx.holdStartTime = now;
            }
            ctx.status = StationStatus.HOLD;
        } else if (!event.holdFlag() && ctx.status == StationStatus.HOLD) {
            if (ctx.holdStartTime != null) {
                ctx.holdSeconds += Duration.between(ctx.holdStartTime, now).getSeconds();
                ctx.holdStartTime = null;
            }
            if (ctx.startTime != null) {
                ctx.status = StationStatus.RUNNING;
            } else {
                ctx.status = StationStatus.IDLE;
            }
        } else if (event.reworkFlag() && ctx.status != StationStatus.IDLE) {
            ctx.status = StationStatus.REWORK;
        } else if (event.bypassFlag()) {
            ctx.status = StationStatus.BYPASS;
            ctx.startTime = null;
            ctx.holdStartTime = null;
        }

        return snapshot(ctx, now, completedActual, cycleStartForLog);
    }

    private long effectiveElapsed(StationContext ctx, OffsetDateTime now) {
        if (ctx.startTime == null) {
            return 0;
        }
        long gross = Duration.between(ctx.startTime, now).getSeconds();
        long activeHold = 0;
        if (ctx.status == StationStatus.HOLD && ctx.holdStartTime != null) {
            activeHold = Duration.between(ctx.holdStartTime, now).getSeconds();
        }
        return Math.max(0, gross - ctx.holdSeconds - activeHold);
    }

    private StationSnapshot snapshot(
            StationContext ctx,
            OffsetDateTime now,
            Long leaveCompletedActual,
            OffsetDateTime leaveCycleStart
    ) {
        if (ctx.status == StationStatus.BYPASS) {
            return new StationSnapshot(
                    ctx.stationCode,
                    ctx.so,
                    ctx.esn,
                    ctx.engineType,
                    ctx.standardCt,
                    0,
                    0,
                    StationStatus.BYPASS,
                    colorOf(StationStatus.BYPASS),
                    now,
                    leaveCompletedActual,
                    leaveCycleStart
            );
        }
        if (ctx.status == StationStatus.ABNORMAL) {
            return new StationSnapshot(
                    ctx.stationCode,
                    ctx.so,
                    ctx.esn,
                    ctx.engineType,
                    ctx.standardCt,
                    0,
                    0,
                    StationStatus.ABNORMAL,
                    colorOf(StationStatus.ABNORMAL),
                    now,
                    leaveCompletedActual,
                    leaveCycleStart
            );
        }
        if (ctx.status == StationStatus.IDLE) {
            return new StationSnapshot(
                    ctx.stationCode,
                    ctx.so,
                    ctx.esn,
                    ctx.engineType,
                    ctx.standardCt,
                    0,
                    0,
                    StationStatus.IDLE,
                    colorOf(StationStatus.IDLE),
                    now,
                    leaveCompletedActual,
                    leaveCycleStart
            );
        }

        long elapsed = effectiveElapsed(ctx, now);
        if (ctx.standardCt > 0
                && (ctx.status == StationStatus.RUNNING
                || ctx.status == StationStatus.WARN
                || ctx.status == StationStatus.ALARM)) {
            double ratio = (double) elapsed / ctx.standardCt;
            if (ratio >= 1.0) {
                ctx.status = StationStatus.OVERTIME;
            } else if (ratio >= ctx.alarmThresholdElapsed) {
                ctx.status = StationStatus.ALARM;
            } else if (ratio >= ctx.warnThresholdElapsed) {
                ctx.status = StationStatus.WARN;
            } else {
                ctx.status = StationStatus.RUNNING;
            }
        }

        long remain = ctx.standardCt > 0 ? (long) ctx.standardCt - elapsed : 0;
        return new StationSnapshot(
                ctx.stationCode,
                ctx.so,
                ctx.esn,
                ctx.engineType,
                ctx.standardCt,
                elapsed,
                remain,
                ctx.status,
                colorOf(ctx.status),
                now,
                leaveCompletedActual,
                leaveCycleStart
        );
    }

    private String colorOf(StationStatus status) {
        return switch (status) {
            case RUNNING -> "GREEN";
            case WARN -> "YELLOW";
            case ALARM -> "RED";
            case OVERTIME -> "RED_FLASH";
            case HOLD -> "BLUE";
            case REWORK -> "PURPLE";
            case BYPASS, IDLE -> "GRAY";
            case ABNORMAL -> "RED";
            default -> "GRAY";
        };
    }
}
