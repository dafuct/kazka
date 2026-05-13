package com.kazka.billing;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyCounterResetJobTest extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired MonthlyCounterResetJob job;

    @Test
    void should_reset_counter_when_last_reset_was_in_previous_month() {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail("monthly-it-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash("x");
        u.setDisplayName("Tester");
        u.setEmailVerified(true);
        u.setStoriesThisMonth(3);
        u.setCounterResetAt(Instant.now().minus(35, ChronoUnit.DAYS));
        users.save(u);

        job.resetCounters();

        User reloaded = users.findById(u.getId()).orElseThrow();
        assertThat(reloaded.getStoriesThisMonth()).isZero();
        assertThat(reloaded.getCounterResetAt()).isAfter(Instant.now().minusSeconds(10));
    }

    @Test
    void should_not_touch_user_already_reset_this_month() {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail("recent-it-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash("x");
        u.setDisplayName("Tester");
        u.setEmailVerified(true);
        u.setStoriesThisMonth(2);
        u.setCounterResetAt(Instant.now());
        users.save(u);

        job.resetCounters();

        User reloaded = users.findById(u.getId()).orElseThrow();
        assertThat(reloaded.getStoriesThisMonth()).isEqualTo(2);
    }
}
