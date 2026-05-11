package com.ccec.timer.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StationWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String stationCode = stationCode(session);
        sessions.put(stationCode, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().remove(session);
    }

    public void push(String stationCode, Object payload) {
        WebSocketSession session = sessions.get(stationCode);
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ignored) {
            sessions.remove(stationCode);
        }
    }

    private String stationCode(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
