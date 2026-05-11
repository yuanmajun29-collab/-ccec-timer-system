package com.ccec.timer.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final JdbcTemplate jdbc;

    public AuditLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String operationType, String objectType, String objectId, String beforeJson, String afterJson) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "SYSTEM";
        String userName = userId;
        Long id = jdbc.queryForObject("SELECT SEQ_AUDIT_LOG_ID.NEXTVAL FROM DUAL", Long.class);
        jdbc.update(
                """
                        INSERT INTO T_AUDIT_LOG (
                          ID, USER_ID, USER_NAME, OPERATION_TYPE, OBJECT_TYPE, OBJECT_ID, BEFORE_VALUE, AFTER_VALUE
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                userId,
                userName,
                operationType,
                objectType,
                objectId,
                beforeJson,
                afterJson
        );
    }
}
