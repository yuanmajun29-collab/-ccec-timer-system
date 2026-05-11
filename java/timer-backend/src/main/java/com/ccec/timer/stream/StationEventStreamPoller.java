package com.ccec.timer.stream;

import com.ccec.timer.config.RedisEventConsumer;
import com.ccec.timer.config.TimerProperties;
import com.ccec.timer.domain.StationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StationEventStreamPoller implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(StationEventStreamPoller.class);

    private final StringRedisTemplate redis;
    private final TimerProperties timerProperties;
    private final ObjectMapper objectMapper;
    private final RedisEventConsumer redisEventConsumer;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private ExecutorService executor;

    public StationEventStreamPoller(
            StringRedisTemplate redis,
            TimerProperties timerProperties,
            ObjectMapper objectMapper,
            RedisEventConsumer redisEventConsumer
    ) {
        this.redis = redis;
        this.timerProperties = timerProperties;
        this.objectMapper = objectMapper;
        this.redisEventConsumer = redisEventConsumer;
    }

    @PostConstruct
    public void start() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ccec-station-event-stream");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::pollLoop);
    }

    private void pollLoop() {
        String streamKey = timerProperties.getRedis().getStreamKey();
        String group = timerProperties.getRedis().getConsumerGroup();
        ensureConsumerGroup(streamKey, group);
        String consumerName = safeHostname() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Redis stream consumer started stream={} group={} name={}", streamKey, group, consumerName);

        while (running.get()) {
            try {
                List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                        Consumer.from(group, consumerName),
                        StreamReadOptions.empty().count(32).block(Duration.ofSeconds(2)),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );
                if (records == null || records.isEmpty()) {
                    continue;
                }
                for (MapRecord<String, Object, Object> rec : records) {
                    processRecord(streamKey, group, rec);
                }
            } catch (Exception e) {
                if (!running.get()) {
                    break;
                }
                log.warn("Stream read error: {}", e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processRecord(String streamKey, String group, MapRecord<String, Object, Object> rec) {
        try {
            Object raw = rec.getValue().get("payload");
            if (raw == null) {
                log.warn("Stream entry {} missing payload field, acking", rec.getId());
                redis.opsForStream().acknowledge(group, rec);
                return;
            }
            StationEvent event = objectMapper.readValue(raw.toString(), StationEvent.class);
            redisEventConsumer.onEvent(event);
            redis.opsForStream().acknowledge(group, rec);
        } catch (Exception e) {
            log.error("Failed to process stream record {}, will not ack (PEL retry)", rec.getId(), e);
        }
    }

    private void ensureConsumerGroup(String streamKey, String group) {
        for (int attempt = 0; attempt < 60 && running.get(); attempt++) {
            try {
                // Spring Data Redis 3.3：两参数 createGroup 使用 latest 偏移，并在流不存在时创建流
                redis.opsForStream().createGroup(streamKey, group);
                log.info("Redis consumer group {} on {} ready", group, streamKey);
                return;
            } catch (Exception ex) {
                if (isBusyGroup(ex)) {
                    return;
                }
                log.warn("Waiting for Redis to create stream group (attempt {}): {}", attempt + 1, ex.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static boolean isBusyGroup(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String m = t.getMessage();
            if (m != null && m.contains("BUSYGROUP")) {
                return true;
            }
        }
        return false;
    }

    private static String safeHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public void destroy() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
