package com.ccec.timer.api.management;

import com.ccec.timer.admin.WorkInstructionManagementService;
import com.ccec.timer.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/management/work-instructions")
public class ManagementWorkInstructionController {
    private final WorkInstructionManagementService workInstructionManagementService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ManagementWorkInstructionController(
            WorkInstructionManagementService workInstructionManagementService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.workInstructionManagementService = workInstructionManagementService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String stationCode) {
        return ApiResponses.ok(Map.of("items", workInstructionManagementService.listAll(stationCode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable long id) {
        return workInstructionManagementService.findById(id)
                .map(row -> ResponseEntity.ok(ApiResponses.ok(row)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        validate(body);
        long id = workInstructionManagementService.create(body);
        auditLogService.log("CREATE", "T_WORK_INSTRUCTION", String.valueOf(id), null, objectMapper.writeValueAsString(body));
        return ResponseEntity.ok(ApiResponses.ok(Map.of("id", id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> body)
            throws JsonProcessingException {
        validate(body);
        String before = workInstructionManagementService.findById(id)
                .map(m -> {
                    try {
                        return objectMapper.writeValueAsString(m);
                    } catch (JsonProcessingException e) {
                        return "{}";
                    }
                })
                .orElse("{}");
        if (!workInstructionManagementService.update(id, body)) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.log("UPDATE", "T_WORK_INSTRUCTION", String.valueOf(id), before, objectMapper.writeValueAsString(body));
        return ResponseEntity.ok(ApiResponses.ok(Map.of("id", id)));
    }

    private static void validate(Map<String, Object> body) {
        ValidationUtil.requireNonBlank(body, "docNo");
        ValidationUtil.requireNonBlank(body, "title");
        ValidationUtil.requireNonBlank(body, "stationCode");
        ValidationUtil.requireNonBlank(body, "versionNo");
        ValidationUtil.requireNonBlank(body, "contentText");
    }
}
