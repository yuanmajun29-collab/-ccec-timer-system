package com.ccec.timer.api.management;

import java.util.Map;

public final class ValidationUtil {
    private ValidationUtil() {}

    public static void requireNonBlank(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
    }
}
