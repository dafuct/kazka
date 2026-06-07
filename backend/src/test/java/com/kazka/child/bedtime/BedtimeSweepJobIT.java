package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.ai.AiClient;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
class BedtimeSweepJobIT extends AbstractIT {

    @Autowired BedtimeSweepJob job;
    @Autowired BedtimeScheduleRepository schedules;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AiClient aiClient;

    @BeforeEach
    void setup() {
        // runOnce() sweeps the whole table, so leftover schedules from other ITs would
        // be counted here; clear them so the zero-count assertions reflect only this test.
        schedules.deleteAll();
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just("Title\n\nOnce upon a time..."));
        when(aiClient.streamEdit(anyString(), anyString())).thenReturn(Flux.just("Title\n\nOnce upon a time..."));
    }

    @Test
    void should_pick_up_due_schedules_and_skip_undue() throws Exception {
        String userId = seedUser();
        String dueId = seedProfile(userId);
        String notYetId = seedProfile(userId);

        BedtimeSchedule due = new BedtimeSchedule();
        due.setChildProfileId(dueId); due.setEnabled(true); due.setThemes(List.of());
        due.setLocalTime("20:30"); due.setTimezone("Europe/Kyiv");
        due.setNextRunAt(Instant.now().minusSeconds(60));
        schedules.save(due);

        BedtimeSchedule notYet = new BedtimeSchedule();
        notYet.setChildProfileId(notYetId); notYet.setEnabled(true); notYet.setThemes(List.of());
        notYet.setLocalTime("20:30"); notYet.setTimezone("Europe/Kyiv");
        notYet.setNextRunAt(Instant.now().plus(Duration.ofHours(2)));
        schedules.save(notYet);

        int picked = job.runOnce();
        assertThat(picked).isEqualTo(1);

        // Wait for the async worker to land the lastSentAt update (no Thread.sleep — Awaitility).
        Awaitility.await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            BedtimeSchedule result = schedules.findByChildProfileId(dueId).orElseThrow();
            assertThat(result.getLastSentAt()).isNotNull();
        });

        BedtimeSchedule untouched = schedules.findByChildProfileId(notYetId).orElseThrow();
        assertThat(untouched.getLastSentAt()).isNull();
    }

    @Test
    void should_skip_outside_one_hour_horizon() {
        String userId = seedUser();
        String stale = seedProfile(userId);
        BedtimeSchedule staleSchedule = new BedtimeSchedule();
        staleSchedule.setChildProfileId(stale); staleSchedule.setEnabled(true); staleSchedule.setThemes(List.of());
        staleSchedule.setLocalTime("20:30"); staleSchedule.setTimezone("Europe/Kyiv");
        staleSchedule.setNextRunAt(Instant.now().minus(Duration.ofHours(2)));  // older than horizon
        schedules.save(staleSchedule);

        int picked = job.runOnce();
        assertThat(picked).isZero();
    }

    @Test
    void should_skip_schedules_for_suspended_users() {
        String userId = seedSuspendedUser();
        String profileId = seedProfile(userId);
        BedtimeSchedule suspendedSchedule = new BedtimeSchedule();
        suspendedSchedule.setChildProfileId(profileId); suspendedSchedule.setEnabled(true); suspendedSchedule.setThemes(List.of());
        suspendedSchedule.setLocalTime("20:30"); suspendedSchedule.setTimezone("Europe/Kyiv");
        suspendedSchedule.setNextRunAt(Instant.now().minusSeconds(60));
        schedules.save(suspendedSchedule);

        int picked = job.runOnce();
        assertThat(picked).isZero();
    }

    private String seedUser() {
        return seedUserInternal(false);
    }

    private String seedSuspendedUser() {
        return seedUserInternal(true);
    }

    private String seedUserInternal(boolean suspended) {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test");
        user.setDisplayName("Parent");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        if (suspended) user.setSuspendedAt(Instant.now());
        users.save(user);
        return id;
    }

    private String seedProfile(String userId) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId); profile.setName("T"); profile.setAvatarSeed("s"); profile.setPreferredLanguage("uk");
        return profiles.save(profile).getId();
    }
}
