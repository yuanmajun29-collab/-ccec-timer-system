package com.ccec.timer.config;

import com.ccec.timer.domain.StationEvent;
import com.ccec.timer.domain.StationSnapshot;
import com.ccec.timer.domain.StationStatus;
import com.ccec.timer.mqtt.MqttStationPublisher;
import com.ccec.timer.persistence.AlarmRecordRepository;
import com.ccec.timer.persistence.ProductionRecordRepository;
import com.ccec.timer.state.StationStateMachine;
import com.ccec.timer.ws.StationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(RedisEventConsumer.class);

    private final StationStateMachine stateMachine;
    private final StationWebSocketHandler webSocketHandler;
    private final StringRedisTemplate redis;
    private final TimerProperties timerProperties;
    private final ObjectMapper objectMapper;
    private final ProductionRecordRepository productionRecordRepository;
    private final AlarmRecordRepository alarmRecordRepository;
    private final MqttStationPublisher mqttStationPublisher;

    private final Map<String, Long> lastEventSeqByStation = new ConcurrentHashMap<>();
    private final Map<String, StationStatus> lastStatusByStation = new ConcurrentHashMap<>();

    public RedisEventConsumer(
            StationStateMachine stateMachine,
            StationWebSocketHandler webSocketHandler,
            StringRedisTemplate redis,
            TimerProperties timerProperties,
            ObjectMapper objectMapper,
            ProductionRecordRepository productionRecordRepository,
            AlarmRecordRepository alarmRecordRepository,
            @Autowired(required = false) MqttStationPublisher mqttStationPublisher
    ) {
        this.stateMachine = stateMachine;
        this.webSocketHandler = webSocketHandler;
        this.redis = redis;
        this.timerProperties = timerProperties;
        this.objectMapper = objectMapper;
        this.productionRecordRepository = productionRecordRepository;
        this.alarmRecordRepository = alarmRecordRepository;
        this.mqttStationPublisher = mqttStationPublisher;
    }

    public void onEvent(StationEvent event) {
        Long prevSeq = lastEventSeqByStation.get(event.stationCode());
        if (prevSeq != null && event.eventSeq() <= prevSeq) {
            return;
        }
        lastEventSeqByStation.put(event.stationCode(), event.eventSeq());

        StationStatus statusBefore = lastStatusByStation.getOrDefault(event.stationCode(), StationStatus.OFFLINE);
        StationSnapshot snapshot = stateMachine.apply(event);

        writeStatusCache(snapshot);
        webSocketHandler.push(event.stationCode(), snapshot);
        if (mqttStationPublisher != null) {
            mqttStationPublisher.publishSnapshot(snapshot);
        }

        if (snapshot.completedActualSeconds() != null && snapshot.cycleStartTime() != null) {
            try {
                productionRecordRepository.insertCompletedCycle(
                        snapshot.stationCode(),
                        snapshot.so(),
                        snapshot.esn(),
                        snapshot.engineType(),
                        snapshot.standardCt(),
                        snapshot.completedActualSeconds(),
                        snapshot.cycleStartTime(),
                        snapshot.serverTime()
                );
            } catch (Exception e) {
                log.warn("Failed to persist production record for station {}", snapshot.stationCode(), e);
            }
        }

        maybeRaiseCtAlarm(snapshot, statusBefore);
        lastStatusByStation.put(event.stationCode(), snapshot.status());
    }

    private void writeStatusCache(StationSnapshot snapshot) {
        try {
            String key = timerProperties.getRedis().getStatusKeyPrefix() + snapshot.stationCode();
            String json = objectMapper.writeValueAsString(snapshot);
            redis.opsForValue().set(key, json, Duration.ofHours(24));
        } catch (Exception e) {
            log.debug("Skip Redis status cache: {}", e.getMessage());
        }
    }

    private void maybeRaiseCtAlarm(StationSnapshot snapshot, StationStatus previous) {
        StationStatus now = snapshot.status();
        boolean criticalNow = now == StationStatus.ALARM || now == StationStatus.OVERTIME;
        boolean criticalBefore = previous == StationStatus.ALARM || previous == StationStatus.OVERTIME;
        if (!criticalNow || criticalBefore) {
            return;
        }
        try {
            String desc = "工位 " + snapshot.stationCode() + " CT 超时风险，状态=" + now
                    + "，已用 " + snapshot.elapsed() + "s / 标准 " + snapshot.standardCt() + "s";
            alarmRecordRepository.insertCtExceeded(snapshot.stationCode(), now, desc);
        } catch (Exception e) {
            log.warn("Failed to persist alarm for station {}", snapshot.stationCode(), e);
        }
    }
}
