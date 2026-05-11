package com.ccec.timer.api.management;

import com.ccec.timer.admin.AlarmManagementService;
import com.ccec.timer.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/management/alarms")
public class ManagementAlarmController {
    private final AlarmManagementService alarmManagementService;
    private final AuditLogService auditLogService;

    public ManagementAlarmController(AlarmManagementService alarmManagementService, AuditLogService auditLogService) {
        this.alarmManagementService = alarmManagementService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String handleStatus) {
        return ApiResponses.ok(Map.of("items", alarmManagementService.list(handleStatus)));
    }

    @PutMapping("/{id}/handle")
    public ResponseEntity<Map<String, Object>> handle(@PathVariable long id, @RequestBody Map<String, Object> body) {
        ValidationUtil.requireNonBlank(body, "handleStatus");
        String status = String.valueOf(body.get("handleStatus")).trim();
        if (!alarmManagementService.updateHandleStatus(id, status)) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.log("HANDLE_ALARM", "T_ALARM_RECORD", String.valueOf(id), null, status);
        return ResponseEntity.ok(ApiResponses.ok(Map.of("id", id, "handleStatus", status)));
    }
}
