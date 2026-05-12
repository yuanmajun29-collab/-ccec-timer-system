package com.ccec.timer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "timer")
public class TimerProperties {
    private final Websocket websocket = new Websocket();
    private final Ct ct = new Ct();
    private final Redis redis = new Redis();
    private final Mqtt mqtt = new Mqtt();

    public Websocket getWebsocket() {
        return websocket;
    }

    public Ct getCt() {
        return ct;
    }

    public Redis getRedis() {
        return redis;
    }

    public Mqtt getMqtt() {
        return mqtt;
    }

    public static class Websocket {
        private int heartbeatSeconds = 30;

        public int getHeartbeatSeconds() {
            return heartbeatSeconds;
        }

        public void setHeartbeatSeconds(int heartbeatSeconds) {
            this.heartbeatSeconds = heartbeatSeconds;
        }
    }

    public static class Ct {
        private int defaultSeconds = 300;

        public int getDefaultSeconds() {
            return defaultSeconds;
        }

        public void setDefaultSeconds(int defaultSeconds) {
            this.defaultSeconds = defaultSeconds;
        }
    }

    public static class Redis {
        /** 与采集端 REDIS_STREAM_KEY / TIMER_REDIS_STREAM_KEY 一致，默认 station:event:queue */
        private String streamKey = "station:event:queue";
        private String consumerGroup = "timer-backend";
        /** 40 工位快照 Hash：hash:station:state，field=工位编码 */
        private String stateHashKey = "hash:station:state";

        public String getStreamKey() {
            return streamKey;
        }

        public void setStreamKey(String streamKey) {
            this.streamKey = streamKey;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public String getStateHashKey() {
            return stateHashKey;
        }

        public void setStateHashKey(String stateHashKey) {
            this.stateHashKey = stateHashKey;
        }
    }

    /**
     * 与 Mosquitto 等 MQTT Broker 对接；安卓一体机原生订阅 topicPrefix/{stationCode}/snapshot。
     */
    public static class Mqtt {
        private boolean enabled = false;
        /** 例如 tcp://mosquitto:1883 */
        private String brokerUri = "tcp://localhost:1883";
        private String username = "";
        private String password = "";
        private String clientIdPrefix = "timer-backend";
        /** 默认 ccec/station → 完整 topic ccec/station/A601/snapshot */
        private String topicPrefix = "ccec/station";
        private int qos = 1;
        /** 保留最新消息，便于终端晚订阅仍拿到上一帧 */
        private boolean retained = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBrokerUri() {
            return brokerUri;
        }

        public void setBrokerUri(String brokerUri) {
            this.brokerUri = brokerUri;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getClientIdPrefix() {
            return clientIdPrefix;
        }

        public void setClientIdPrefix(String clientIdPrefix) {
            this.clientIdPrefix = clientIdPrefix;
        }

        public String getTopicPrefix() {
            return topicPrefix;
        }

        public void setTopicPrefix(String topicPrefix) {
            this.topicPrefix = topicPrefix;
        }

        public int getQos() {
            return qos;
        }

        public void setQos(int qos) {
            this.qos = qos;
        }

        public boolean isRetained() {
            return retained;
        }

        public void setRetained(boolean retained) {
            this.retained = retained;
        }
    }
}
