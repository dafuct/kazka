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
        BedtimeSchedule s = enabledSchedule(profileId);
        s.setLocalTime("19:45");
        s.setTimezone("Europe/Warsaw");
        s.setThemes(List.of("dragons"));
        schedules.save(s);

        when(entitlements.isPro(userId)).thenReturn(false);
        events.publishEvent(new EntitlementDowngradedEvent(userId));

        BedtimeSchedule reloaded = schedules.findByChildProfileId(profileId).orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();
        assertThat(reloaded.getLocalTime()).isEqualTo("19:45");
        assertThat(reloaded.getTimezone()).isEqualTo("Europe/Warsaw");
        assertThat(reloaded.getThemes()).containsExactly("dragons");
    }

    private BedtimeSchedule enabledSchedule(String profileId) {
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(profileId);
        s.setEnabled(true);
        s.setLocalTime("20:30");
        s.setTimezone("Europe/Kyiv");
        s.setThemes(List.of());
        s.setNextRunAt(Instant.now().plusSeconds(3600));
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

    private String seedProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId); p.setName("T"); p.setAvatarSeed("s"); p.setPreferredLanguage("uk");
        return profiles.save(p).getId();
    }
}
