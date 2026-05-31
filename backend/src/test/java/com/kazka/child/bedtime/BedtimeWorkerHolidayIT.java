package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.ai.AiClient;
import com.kazka.holidays.Holiday;
import com.kazka.holidays.HolidayCalendar;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
class BedtimeWorkerHolidayIT extends AbstractIT {

    @Autowired BedtimeWorker worker;
    @Autowired BedtimeScheduleRepository schedules;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AiClient aiClient;
    @MockitoBean EntitlementResolver entitlements;
    @MockitoBean HolidayCalendar holidayCalendar;

    String userId;
    String profileId;

    @BeforeEach
    void setup() {
        when(entitlements.isPro(anyString())).thenReturn(true);
        when(aiClient.streamText(anyString(), anyString())).thenReturn(
                Flux.just("Bedtime Title\n\nOnce upon a time, a child named Test went to sleep peacefully."));
        when(aiClient.streamEdit(anyString(), anyString())).thenReturn(
                Flux.just("Bedtime Title\n\nOnce upon a time, a child named Test went to sleep peacefully."));

        userId = seedUser("parent-" + UUID.randomUUID() + "@test");
        profileId = seedProfile(userId);
    }

    @Test
    void should_inject_holiday_theme_when_active_and_opted_in() throws Exception {
        when(holidayCalendar.activeFor(any(), any())).thenReturn(Optional.of(Holiday.CHRISTMAS));

        BedtimeSchedule s = enabledSchedule(profileId, true);
        schedules.save(s);

        worker.enqueueAsync(profileId).get();

        Story story = stories.findAllByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);
        assertThat(story.getTheme()).contains("Різдво");
    }

    @Test
    void should_use_normal_theme_when_holiday_active_but_opted_out() throws Exception {
        when(holidayCalendar.activeFor(any(), any())).thenReturn(Optional.of(Holiday.CHRISTMAS));

        BedtimeSchedule s = enabledSchedule(profileId, false);
        s.setThemes(List.of("dragons"));
        schedules.save(s);

        worker.enqueueAsync(profileId).get();

        Story story = stories.findAllByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);
        assertThat(story.getTheme()).contains("dragons");
        assertThat(story.getTheme()).doesNotContain("Різдво");
    }

    @Test
    void should_use_normal_theme_when_no_holiday_active() throws Exception {
        when(holidayCalendar.activeFor(any(), any())).thenReturn(Optional.empty());

        BedtimeSchedule s = enabledSchedule(profileId, true);
        s.setThemes(List.of("космос"));
        schedules.save(s);

        worker.enqueueAsync(profileId).get();

        Story story = stories.findAllByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);
        assertThat(story.getTheme()).contains("космос");
    }

    private BedtimeSchedule enabledSchedule(String childProfileId, boolean holidayThemes) {
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(childProfileId);
        s.setEnabled(true);
        s.setLocalTime("20:30");
        s.setTimezone("Europe/Kyiv");
        s.setThemes(List.of());
        s.setHolidayThemesEnabled(holidayThemes);
        s.setNextRunAt(Instant.now().minusSeconds(60));
        return s;
    }

    private String seedUser(String email) {
        String id = UUID.randomUUID().toString();
        User u = new User();
        u.setId(id); u.setEmail(email); u.setDisplayName("Parent");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER); u.setEmailVerified(true);
        users.save(u);
        return id;
    }

    private String seedProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId); p.setName("Test"); p.setAvatarSeed("s"); p.setPreferredLanguage("uk");
        p.setInterests(List.of());
        return profiles.save(p).getId();
    }
}
