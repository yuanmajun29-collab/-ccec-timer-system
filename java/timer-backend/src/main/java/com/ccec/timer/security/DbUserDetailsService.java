package com.ccec.timer.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;

    public DbUserDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<UserAccountRow> rows = jdbc.query(
                """
                        SELECT u.USERNAME, u.PASSWORD_HASH, u.ENABLED, a.AUTHORITY
                        FROM T_SECURITY_USER u
                        JOIN T_SECURITY_AUTHORITY a ON a.USERNAME = u.USERNAME
                        WHERE u.USERNAME = ?
                        """,
                (rs, i) -> new UserAccountRow(
                        rs.getString("USERNAME"),
                        rs.getString("PASSWORD_HASH"),
                        rs.getInt("ENABLED") == 1,
                        rs.getString("AUTHORITY")
                ),
                username
        );
        if (rows.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }
        boolean enabled = rows.get(0).enabled();
        String password = rows.get(0).passwordHash();
        String[] authorities = rows.stream().map(UserAccountRow::authority).distinct().toArray(String[]::new);
        return User.builder()
                .username(username)
                .password(password)
                .disabled(!enabled)
                .authorities(authorities)
                .build();
    }

    private record UserAccountRow(String username, String passwordHash, boolean enabled, String authority) {}
}
