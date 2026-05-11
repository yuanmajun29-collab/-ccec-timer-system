package com.ccec.timer.api.management;

import com.ccec.timer.admin.AuditQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/management/audit-logs")
public class ManagementAuditController {
    private final AuditQueryService auditQueryService;

    public ManagementAuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public Map<String, Object> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponses.ok(auditQueryService.page(page, size));
    }
}
