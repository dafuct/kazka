package com.kazka.moderation;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class ModerationCleanupJobIT extends AbstractIT {

    @Autowired ModerationCleanupJob job;
    @Autowired FlaggedAttemptRepository flags;
    @Autowired UserRepository users;

    private String userId;

    @BeforeEach
    void clean() {
        flags.deleteAll();
        users.deleteAll();
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail("janitor@example.com");
        u.setDisplayName("J");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
        userId = u.getId();
    }

    @Test
    void should_deleteRowsOlderThan90Days_when_jobRuns() {
        save(Instant.now().minus(91, ChronoUnit.DAYS));
        save(Instant.now().minus(89, ChronoUnit.DAYS));
        save(Instant.now());
        assertThat(flags.findAll()).hasSize(3);

        job.runCleanup();

        assertThat(flags.findAll()).hasSize(2);
    }

    private void save(Instant when) {
        FlaggedAttempt fa = new FlaggedAttempt();
        fa.setId(UUID.randomUUID().toString());
        fa.setUserId(userId);
        fa.setPipeline(ModerationPipeline.TEXT_INPUT);
        fa.setCategory(ModerationCategory.SEXUAL);
        fa.setLanguage("uk");
        fa.setPromptText("x");
        fa.setCreatedAt(when);
        flags.save(fa);
    }
}
