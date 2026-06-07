package com.kazka.child.bedtime;

import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileService;
import com.kazka.child.bedtime.dto.BedtimeUpdateRequest;
import com.kazka.story.exception.PaywallRequiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BedtimeScheduleServiceTest {

    @Mock BedtimeScheduleRepository repo;
    @Mock ChildProfileService profiles;
    @Mock com.kazka.billing.EntitlementResolver entitlements;
    @Mock NextRunCalculator nextRun;
    @InjectMocks BedtimeScheduleService svc;

    private ChildProfile owned() {
        ChildProfile profile = new ChildProfile();
        profile.setId("p1"); profile.setUserId("u");
        return profile;
    }

    @Test
    void should_create_schedule_on_first_save() {
        when(profiles.requireOwned("p1", "u")).thenReturn(owned());
        when(entitlements.isPro("u")).thenReturn(true);
        when(repo.findByChildProfileId("p1")).thenReturn(Optional.empty());
        when(repo.save(any(BedtimeSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(nextRun.nextRun(any(), any(), any())).thenReturn(java.time.Instant.parse("2026-06-15T17:30:00Z"));

        BedtimeSchedule saved = svc.upsert("p1", "u",
                new BedtimeUpdateRequest(true, "20:30", "Europe/Kyiv", List.of("dragons"), true));

        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getLocalTime()).isEqualTo("20:30");
        assertThat(saved.getTimezone()).isEqualTo("Europe/Kyiv");
        assertThat(saved.getThemes()).containsExactly("dragons");
        assertThat(saved.getNextRunAt()).isNotNull();
    }

    @Test
    void should_return402_when_freeTier_enables() {
        when(profiles.requireOwned("p1", "u")).thenReturn(owned());
        when(entitlements.isPro("u")).thenReturn(false);

        assertThatThrownBy(() -> svc.upsert("p1", "u",
                new BedtimeUpdateRequest(true, "20:30", "Europe/Kyiv", List.of(), true)))
                .isInstanceOf(PaywallRequiredException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void should_allow_freeTier_to_disable() {
        when(profiles.requireOwned("p1", "u")).thenReturn(owned());
        // entitlements.isPro is NOT called when enabled=false — no stub needed
        when(repo.findByChildProfileId("p1")).thenReturn(Optional.empty());
        when(repo.save(any(BedtimeSchedule.class))).thenAnswer(i -> i.getArgument(0));

        BedtimeSchedule saved = svc.upsert("p1", "u",
                new BedtimeUpdateRequest(false, "20:30", "Europe/Kyiv", List.of(), true));

        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getNextRunAt()).isNull();
    }

    @Test
    void should_reject_invalid_timezone() {
        when(profiles.requireOwned("p1", "u")).thenReturn(owned());
        when(entitlements.isPro("u")).thenReturn(true);

        assertThatThrownBy(() -> svc.upsert("p1", "u",
                new BedtimeUpdateRequest(true, "20:30", "Mars/Olympus", List.of(), true)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repo, never()).save(any());
    }

    @Test
    void should_recompute_next_run_at_on_time_change() {
        BedtimeSchedule existing = new BedtimeSchedule();
        existing.setChildProfileId("p1");
        existing.setEnabled(true);
        existing.setLocalTime("19:00");
        existing.setTimezone("Europe/Kyiv");
        existing.setThemes(List.of());
        when(profiles.requireOwned("p1", "u")).thenReturn(owned());
        when(entitlements.isPro("u")).thenReturn(true);
        when(repo.findByChildProfileId("p1")).thenReturn(Optional.of(existing));
        when(repo.save(any(BedtimeSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(nextRun.nextRun(any(), any(), any())).thenReturn(java.time.Instant.parse("2026-06-15T18:00:00Z"));

        BedtimeSchedule saved = svc.upsert("p1", "u",
                new BedtimeUpdateRequest(true, "21:00", "Europe/Kyiv", List.of(), true));

        ArgumentCaptor<BedtimeSchedule> captor = ArgumentCaptor.forClass(BedtimeSchedule.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getLocalTime()).isEqualTo("21:00");
        assertThat(captor.getValue().getNextRunAt()).isNotNull();
    }

    @Test
    void should_clear_next_run_at_when_disabled() {
        BedtimeSchedule existing = new BedtimeSchedule();
        existing.setChildProfileId("p1");
        existing.setEnabled(true);
        existing.setLocalTime("20:30");
        existing.setTimezone("Europe/Kyiv");
        existing.setThemes(List.of());
        existing.setNextRunAt(java.time.Instant.now().plusSeconds(60));
        when(profiles.requireOwned("p1", "u")).thenReturn(owned());
        // entitlements.isPro is NOT called when enabled=false — no stub needed
        when(repo.findByChildProfileId("p1")).thenReturn(Optional.of(existing));
        when(repo.save(any(BedtimeSchedule.class))).thenAnswer(i -> i.getArgument(0));

        BedtimeSchedule saved = svc.upsert("p1", "u",
                new BedtimeUpdateRequest(false, "20:30", "Europe/Kyiv", List.of(), true));

        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getNextRunAt()).isNull();
    }

    @Test
    void should_persist_holidayThemesEnabled_false_when_user_opts_out() {
        when(profiles.requireOwned("p1", "u")).thenReturn(owned());
        when(entitlements.isPro("u")).thenReturn(true);
        when(repo.findByChildProfileId("p1")).thenReturn(Optional.empty());
        when(repo.save(any(BedtimeSchedule.class))).thenAnswer(i -> i.getArgument(0));
        when(nextRun.nextRun(any(), any(), any())).thenReturn(java.time.Instant.now().plusSeconds(3600));

        BedtimeSchedule saved = svc.upsert("p1", "u",
                new BedtimeUpdateRequest(true, "20:30", "Europe/Kyiv", List.of(), false));

        assertThat(saved.isHolidayThemesEnabled()).isFalse();
    }
}
