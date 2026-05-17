package com.ccec.timer.api;

import com.ccec.timer.admin.WorkInstructionManagementService;
import com.ccec.timer.api.management.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/stations")
public class PublicWorkInstructionController {
    private final WorkInstructionManagementService workInstructionManagementService;

    public PublicWorkInstructionController(WorkInstructionManagementService workInstructionManagementService) {
        this.workInstructionManagementService = workInstructionManagementService;
    }

    @GetMapping("/{stationCode}/work-instruction")
    public ResponseEntity<Map<String, Object>> latestByStation(@PathVariable String stationCode) {
        return workInstructionManagementService.findLatestEnabledByStation(stationCode)
                .map(row -> ResponseEntity.ok(ApiResponses.ok(row)))
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "code", "404",
                        "message", "WORK_INSTRUCTION_NOT_FOUND"
                )));
    }
}
