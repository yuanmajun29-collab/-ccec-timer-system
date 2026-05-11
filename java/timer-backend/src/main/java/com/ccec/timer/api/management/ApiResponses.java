package com.ccec.timer.api.management;

import java.util.Map;

public final class ApiResponses {
    private ApiResponses() {}

    public static Map<String, Object> ok(Object data) {
        return Map.of("success", true, "code", "0", "message", "OK", "data", data);
    }
}
