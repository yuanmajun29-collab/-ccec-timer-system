package com.ccec.timer.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class StationWebSocketConfig implements WebSocketConfigurer {
    private final StationWebSocketHandler handler;

    public StationWebSocketConfig(StationWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/station/{stationCode}")
                .setAllowedOrigins("*");
    }
}
