package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class BedtimeScheduleRepositoryIT extends AbstractIT {

    @Autowired BedtimeScheduleRepository repo;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void should_round_trip_themes_json() {
        String profileId = seedProfile();
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(profileId);
        s.setEnabled(true);
        s.setThemes(List.of("dragons", "коти"));
        s.setNextRunAt(Instant.now().plusSeconds(60));
        repo.save(s);

        BedtimeSchedule loaded = repo.findByChildProfileId(profileId).orElseThrow();
        assertThat(loaded.getThemes()).containsExactly("dragons", "коти");
    }

    @Test
    void should_find_due_schedules_within_window() {
        String profileId = seedProfile();
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(profileId);
        s.setEnabled(true);
        s.setThemes(List.of());
        s.setNextRunAt(Instant.now().minusSeconds(60));
        repo.save(s);

        Instant now = Instant.now();
        Instant horizon = now.minus(Duration.ofHours(1));
        List<BedtimeSchedule> due = repo.findDueForSweep(now, horizon);
        assertThat(due).extracting(BedtimeSchedule::getChildProfileId).contains(profileId);
    }

    @Test
    void should_skip_disabled_schedules() {
        String profileId = seedProfile();
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(profileId);
        s.setEnabled(false);
        s.setThemes(List.of());
        s.setNextRunAt(Instant.now().minusSeconds(60));
        repo.save(s);

        List<BedtimeSchedule> due = repo.findDueForSweep(Instant.now(), Instant.now().minus(Duration.ofHours(1)));
        assertThat(due).extracting(BedtimeSchedule::getChildProfileId).doesNotContain(profileId);
    }

    @Test
    void should_skip_when_outside_one_hour_horizon() {
        String profileId = seedProfile();
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(profileId);
        s.setEnabled(true);
        s.setThemes(List.of());
        s.setNextRunAt(Instant.now().minus(Duration.ofHours(2)));
        repo.save(s);

        List<BedtimeSchedule> due = repo.findDueForSweep(Instant.now(), Instant.now().minus(Duration.ofHours(1)));
        assertThat(due).extracting(BedtimeSchedule::getChildProfileId).doesNotContain(profileId);
    }

    @Test
    void should_disable_all_schedules_for_a_user() {
        String userId = seedUser();
        String profileA = seedProfile(userId);
        String profileB = seedProfile(userId);
        repo.save(enabledSchedule(profileA));
        repo.save(enabledSchedule(profileB));

        int n = repo.disableAllForUser(userId);
        assertThat(n).isEqualTo(2);
        assertThat(repo.findByChildProfileId(profileA).orElseThrow().isEnabled()).isFalse();
        assertThat(repo.findByChildProfileId(profileB).orElseThrow().isEnabled()).isFalse();
    }

    private BedtimeSchedule enabledSchedule(String profileId) {
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(profileId);
        s.setEnabled(true);
        s.setThemes(List.of());
        return s;
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User u = new User();
        u.setId(id);
        u.setEmail(id + "@test");
        u.setDisplayName("T");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
        return id;
    }

    private String seedProfile() {
        return seedProfile(seedUser());
    }

    private String seedProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName("T");
        p.setAvatarSeed("s");
        p.setPreferredLanguage("uk");
        return profiles.save(p).getId();
    }
}
