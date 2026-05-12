package com.ccec.timer.mqtt;

import com.ccec.timer.config.TimerProperties;
import com.ccec.timer.ws.StateUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 将工位快照发布到 MQTT，供安卓一体机等原生终端订阅（与 WebSocket 并行）。
 */
@Component
@ConditionalOnProperty(prefix = "timer.mqtt", name = "enabled", havingValue = "true")
public class MqttStationPublisher {
    private static final Logger log = LoggerFactory.getLogger(MqttStationPublisher.class);

    private final TimerProperties timerProperties;
    private final ObjectMapper objectMapper;
    private MqttClient client;

    public MqttStationPublisher(TimerProperties timerProperties, ObjectMapper objectMapper) {
        this.timerProperties = timerProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void connect() {
        TimerProperties.Mqtt mqttProps = timerProperties.getMqtt();
        try {
            String clientId = mqttProps.getClientIdPrefix() + "-" + UUID.randomUUID().toString().substring(0, 8);
            client = new MqttClient(mqttProps.getBrokerUri(), clientId, new MemoryPersistence());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            if (mqttProps.getUsername() != null && !mqttProps.getUsername().isBlank()) {
                opts.setUserName(mqttProps.getUsername());
                opts.setPassword(mqttProps.getPassword() != null ? mqttProps.getPassword().toCharArray() : new char[0]);
            }
            client.connect(opts);
            log.info("MQTT broker connected: {}", mqttProps.getBrokerUri());
        } catch (MqttException e) {
            log.error("MQTT connect failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException ignored) {
            }
        }
    }

    /** 与工位 WSS 使用同一 JSON 模型（如 {@link com.ccec.timer.ws.StateUpdateMessage}），便于安卓/WebView 统一解析。 */
    public void publishJson(Object payload) {
        TimerProperties.Mqtt mqttProps = timerProperties.getMqtt();
        if (client == null || !client.isConnected()) {
            tryConnectOnce();
        }
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            String stationCode = extractStationCode(payload);
            String topic = mqttProps.getTopicPrefix() + "/" + stationCode + "/snapshot";
            byte[] bytes = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            MqttMessage msg = new MqttMessage(bytes);
            msg.setQos(mqttProps.getQos());
            msg.setRetained(mqttProps.isRetained());
            client.publish(topic, msg);
        } catch (Exception e) {
            log.warn("MQTT publish failed: {}", e.getMessage());
        }
    }

    private static String extractStationCode(Object payload) {
        if (payload instanceof StateUpdateMessage m && m.stationCode != null) {
            return m.stationCode;
        }
        throw new IllegalArgumentException("Unsupported MQTT payload type: " + payload.getClass().getName());
    }

    private void tryConnectOnce() {
        try {
            if (client != null && !client.isConnected()) {
                client.reconnect();
            }
        } catch (Exception e) {
            log.debug("MQTT reconnect: {}", e.getMessage());
        }
    }
}
