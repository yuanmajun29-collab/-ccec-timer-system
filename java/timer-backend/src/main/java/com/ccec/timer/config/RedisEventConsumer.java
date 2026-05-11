package com.ccec.timer.config;

import com.ccec.timer.domain.StationEvent;
import com.ccec.timer.state.StationStateMachine;
import com.ccec.timer.ws.StationWebSocketHandler;
import org.springframework.stereotype.Component;

@Component
public class RedisEventConsumer {
    private final StationStateMachine stateMachine;
    private final StationWebSocketHandler webSocketHandler;

    public RedisEventConsumer(StationStateMachine stateMachine, StationWebSocketHandler webSocketHandler) {
        this.stateMachine = stateMachine;
        this.webSocketHandler = webSocketHandler;
    }

    public void onEvent(StationEvent event) {
        var snapshot = stateMachine.apply(event);
        webSocketHandler.push(event.stationCode(), snapshot);
        // TODO: 写 Redis station:status:{stationCode}，按状态生成告警，离站时写 T_PRODUCTION_RECORD。
    }
}
