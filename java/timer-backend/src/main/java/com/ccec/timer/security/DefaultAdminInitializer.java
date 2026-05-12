package com.ccec.timer.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动写入默认管理员（方案中 LDAP/AD 集成时可移除此初始化，改为域账号同步）。
 */
@Component
@Order(100)
@ConditionalOnProperty(prefix = "timer.security.default-admin", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DefaultAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM T_SECURITY_USER", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        long id = jdbc.queryForObject("SELECT SEQ_SECURITY_USER_ID.NEXTVAL FROM DUAL", Long.class);
        String hash = passwordEncoder.encode("Admin123!");
        jdbc.update(
                "INSERT INTO T_SECURITY_USER (ID, USERNAME, PASSWORD_HASH, DISPLAY_NAME, ENABLED) VALUES (?, ?, ?, ?, 1)",
                id, "admin", hash, "Administrator"
        );
        jdbc.update(
                "INSERT INTO T_SECURITY_AUTHORITY (USERNAME, AUTHORITY) VALUES (?, ?)",
                "admin", "ROLE_ADMIN"
        );
        log.warn("Default admin user created from configuration; change password before production.");
    }
}
