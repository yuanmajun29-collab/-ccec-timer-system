package com.ccec.timer.api.management;

import com.ccec.timer.admin.CtConfigManagementService;
import com.ccec.timer.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/management/ct-configs")
public class ManagementCtController {
    private final CtConfigManagementService ctConfigManagementService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ManagementCtController(
            CtConfigManagementService ctConfigManagementService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.ctConfigManagementService = ctConfigManagementService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String stationCode) {
        return ApiResponses.ok(Map.of("items", ctConfigManagementService.listAll(stationCode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable long id) {
        return ctConfigManagementService.findById(id)
                .map(row -> ResponseEntity.ok(ApiResponses.ok(row)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        validateCt(body);
        long id = ctConfigManagementService.create(body);
        auditLogService.log("CREATE", "T_CT_CONFIG", String.valueOf(id), null, objectMapper.writeValueAsString(body));
        return ResponseEntity.ok(ApiResponses.ok(Map.of("id", id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> body)
            throws JsonProcessingException {
        validateCt(body);
        String before = ctConfigManagementService.findById(id)
                .map(m -> {
                    try {
                        return objectMapper.writeValueAsString(m);
                    } catch (JsonProcessingException e) {
                        return "{}";
                    }
                })
                .orElse("{}");
        if (!ctConfigManagementService.update(id, body)) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.log("UPDATE", "T_CT_CONFIG", String.valueOf(id), before, objectMapper.writeValueAsString(body));
        return ResponseEntity.ok(ApiResponses.ok(Map.of("id", id)));
    }

    private static void validateCt(Map<String, Object> body) {
        ValidationUtil.requireNonBlank(body, "configVersion");
        ValidationUtil.requireNonBlank(body, "stationCode");
        if (body.get("standardCt") == null) {
            throw new IllegalArgumentException("Missing field: standardCt");
        }
        if (body.get("warnThreshold") == null || body.get("alarmThreshold") == null) {
            throw new IllegalArgumentException("Missing threshold");
        }
        if (body.get("effectiveTime") == null) {
            throw new IllegalArgumentException("Missing field: effectiveTime");
        }
    }
}
