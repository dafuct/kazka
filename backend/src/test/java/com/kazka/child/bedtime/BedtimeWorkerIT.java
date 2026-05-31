package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.ai.AiClient;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
class BedtimeWorkerIT extends AbstractIT {

    @Autowired BedtimeWorker worker;
    @Autowired BedtimeScheduleRepository schedules;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AiClient aiClient;
    @MockitoBean EntitlementResolver entitlements;

    String userId;
    String profileId;

    @BeforeEach
    void setup() {
        when(entitlements.isPro(anyString())).thenReturn(true);
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just("Bedtime Title\n\nOnce upon a time, a child named Test went to sleep peacefully."));
        when(aiClient.streamEdit(anyString(), anyString())).thenReturn(Flux.just("Bedtime Title\n\nOnce upon a time, a child named Test went to sleep peacefully."));

        userId = seedUser("parent-" + UUID.randomUUID() + "@test");
        profileId = seedProfile(userId);
    }

    @Test
    void should_generate_send_and_advance_on_happy_path() throws Exception {
        schedules.save(enabledSchedule(profileId));

        worker.enqueueAsync(profileId).get();

        BedtimeSchedule reloaded = schedules.findByChildProfileId(profileId).orElseThrow();
        assertThat(reloaded.getLastSentAt()).isNotNull();
        assertThat(reloaded.getRetryCount()).isZero();
        assertThat(reloaded.getNextRunAt()).isAfter(Instant.now());

        var rows = stories.findAllByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getChildProfileId()).isEqualTo(profileId);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        String body = received[0].getContent().toString();
        assertThat(body).contains("/stories/" + rows.get(0).getId());
    }

    @Test
    void should_dedup_when_already_sent_today() throws Exception {
        BedtimeSchedule s = enabledSchedule(profileId);
        s.setLastSentAt(Instant.now());
        schedules.save(s);

        worker.enqueueAsync(profileId).get();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(0);
    }

    @Test
    void should_bump_retry_on_failure_and_back_off_15_minutes() throws Exception {
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.error(new RuntimeException("LLM down")));
        schedules.save(enabledSchedule(profileId));

        worker.enqueueAsync(profileId).get();

        BedtimeSchedule reloaded = schedules.findByChildProfileId(profileId).orElseThrow();
        assertThat(reloaded.getRetryCount()).isEqualTo(1);
        assertThat(reloaded.getFailedAt()).isNull();
        assertThat(reloaded.getNextRunAt()).isBetween(
                Instant.now().plusSeconds(60 * 14),
                Instant.now().plusSeconds(60 * 16));
    }

    @Test
    void should_mark_failed_after_three_retries() throws Exception {
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.error(new RuntimeException("LLM down")));
        BedtimeSchedule s = enabledSchedule(profileId);
        s.setRetryCount(2);
        schedules.save(s);

        worker.enqueueAsync(profileId).get();

        BedtimeSchedule reloaded = schedules.findByChildProfileId(profileId).orElseThrow();
        assertThat(reloaded.getRetryCount()).isEqualTo(3);
        assertThat(reloaded.getFailedAt()).isNotNull();
        assertThat(reloaded.getNextRunAt()).isAfter(Instant.now().plusSeconds(60 * 60));
    }

    @Test
    void should_disable_when_user_no_longer_pro() throws Exception {
        when(entitlements.isPro(userId)).thenReturn(false);
        schedules.save(enabledSchedule(profileId));

        worker.enqueueAsync(profileId).get();

        BedtimeSchedule reloaded = schedules.findByChildProfileId(profileId).orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();
    }

    private BedtimeSchedule enabledSchedule(String childProfileId) {
        BedtimeSchedule s = new BedtimeSchedule();
        s.setChildProfileId(childProfileId);
        s.setEnabled(true);
        s.setLocalTime("20:30");
        s.setTimezone("Europe/Kyiv");
        s.setThemes(List.of("dragons"));
        s.setNextRunAt(Instant.now().minusSeconds(60));
        return s;
    }

    private String seedUser(String email) {
        String id = UUID.randomUUID().toString();
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setDisplayName("Parent");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
        return id;
    }

    private String seedProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId); p.setName("Test"); p.setAvatarSeed("s"); p.setPreferredLanguage("uk");
        p.setInterests(List.of("dragons"));
        return profiles.save(p).getId();
    }
}
