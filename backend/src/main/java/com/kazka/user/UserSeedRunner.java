package com.kazka.user;

import com.kazka.auth.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserSeedRunner implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;

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
        User adminUser = new User();
        adminUser.setId(UUID.randomUUID().toString());
        adminUser.setEmail(normalized);
        adminUser.setPasswordHash(passwordEncoder.encode(password));
        adminUser.setDisplayName("Admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEmailVerified(true);
        users.save(adminUser);
        log.info("Seeded admin user {}", normalized);
    }
}
