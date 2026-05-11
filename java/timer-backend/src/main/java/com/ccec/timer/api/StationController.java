package com.ccec.timer.api;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {
    @GetMapping
    public Map<String, Object> list() {
        return Map.of("success", true, "code", "0", "message", "OK", "data", Map.of("items", java.util.List.of()));
    }
}
