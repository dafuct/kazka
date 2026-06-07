package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementDowngradedEvent;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("integration")
class BedtimeDowngradeListenerIT extends AbstractIT {

    @Autowired ApplicationEventPublisher events;
    @Autowired BedtimeScheduleRepository schedules;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EntitlementResolver entitlements;

    @Test
    void should_disable_all_bedtime_schedules_when_user_downgrades() {
        String userId = seedUser();
        String profileA = seedProfile(userId);
        String profileB = seedProfile(userId);

        schedules.save(enabledSchedule(profileA));
        schedules.save(enabledSchedule(profileB));

        when(entitlements.isPro(userId)).thenReturn(false);
        events.publishEvent(new EntitlementDowngradedEvent(userId));

        assertThat(schedules.findByChildProfileId(profileA).orElseThrow().isEnabled()).isFalse();
        assertThat(schedules.findByChildProfileId(profileB).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    void should_preserve_local_time_and_timezone_on_disable() {
        String userId = seedUser();
        String profileId = seedProfile(userId);
        BedtimeSchedule schedule = enabledSchedule(profileId);
        schedule.setLocalTime("19:45");
        schedule.setTimezone("Europe/Warsaw");
        schedule.setThemes(List.of("dragons"));
        schedules.save(schedule);

        when(entitlements.isPro(userId)).thenReturn(false);
        events.publishEvent(new EntitlementDowngradedEvent(userId));

        BedtimeSchedule reloaded = schedules.findByChildProfileId(profileId).orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();
        assertThat(reloaded.getLocalTime()).isEqualTo("19:45");
        assertThat(reloaded.getTimezone()).isEqualTo("Europe/Warsaw");
        assertThat(reloaded.getThemes()).containsExactly("dragons");
    }

    private BedtimeSchedule enabledSchedule(String profileId) {
        BedtimeSchedule schedule = new BedtimeSchedule();
        schedule.setChildProfileId(profileId);
        schedule.setEnabled(true);
        schedule.setLocalTime("20:30");
        schedule.setTimezone("Europe/Kyiv");
        schedule.setThemes(List.of());
        schedule.setNextRunAt(Instant.now().plusSeconds(3600));
        return schedule;
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test");
        user.setDisplayName("T");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
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
