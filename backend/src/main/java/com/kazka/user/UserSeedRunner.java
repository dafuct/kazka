package com.kazka.user;

import com.kazka.auth.AuthProperties;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class UserSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeedRunner.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;

    public UserSeedRunner(UserRepository users, PasswordEncoder passwordEncoder, AuthProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        String email = props.admin() == null ? null : props.admin().email();
        String password = props.admin() == null ? null : props.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("Admin seeding skipped — ADMIN_EMAIL/ADMIN_PASSWORD not set");
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            log.info("Admin user {} already present — skipping seed", normalized);
            return;
        }
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(normalized);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setDisplayName("Admin");
        u.setRole(UserRole.ADMIN);
        u.setEmailVerified(true);
        users.save(u);
        log.info("Seeded admin user {}", normalized);
    }
}
