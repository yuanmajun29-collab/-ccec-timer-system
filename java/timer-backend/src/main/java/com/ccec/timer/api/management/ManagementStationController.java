package com.ccec.timer.api.management;

import com.ccec.timer.admin.StationManagementService;
import com.ccec.timer.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/management/stations")
public class ManagementStationController {
    private final StationManagementService stationManagementService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ManagementStationController(
            StationManagementService stationManagementService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.stationManagementService = stationManagementService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Map<String, Object> list() {
        return ApiResponses.ok(Map.of("items", stationManagementService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable long id) {
        return stationManagementService.findById(id)
                .map(row -> ResponseEntity.ok(ApiResponses.ok(row)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        ValidationUtil.requireNonBlank(body, "stationCode");
        ValidationUtil.requireNonBlank(body, "stationName");
        ValidationUtil.requireNonBlank(body, "lineCode");
        ValidationUtil.requireNonBlank(body, "plcCode");
        long id = stationManagementService.create(body);
        auditLogService.log("CREATE", "T_STATION", String.valueOf(id), null, objectMapper.writeValueAsString(body));
        return ResponseEntity.ok(ApiResponses.ok(Map.of("id", id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> body)
            throws JsonProcessingException {
        ValidationUtil.requireNonBlank(body, "stationCode");
        ValidationUtil.requireNonBlank(body, "stationName");
        ValidationUtil.requireNonBlank(body, "lineCode");
        ValidationUtil.requireNonBlank(body, "plcCode");
        String before = stationManagementService.findById(id)
                .map(m -> {
                    try {
                        return objectMapper.writeValueAsString(m);
                    } catch (JsonProcessingException e) {
                        return "{}";
                    }
                })
                .orElse("{}");
        if (!stationManagementService.update(id, body)) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.log("UPDATE", "T_STATION", String.valueOf(id), before, objectMapper.writeValueAsString(body));
        return ResponseEntity.ok(ApiResponses.ok(Map.of("id", id)));
    }

}
