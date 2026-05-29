package com.kazka.dashboard;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.child.bedtime.BedtimeSchedule;
import com.kazka.child.bedtime.BedtimeScheduleRepository;
import com.kazka.dashboard.dto.DashboardDto;
import com.kazka.illustration.ImageUrlResolver;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock StoryRepository stories;
    @Mock ChildProfileRepository childProfiles;
    @Mock BedtimeScheduleRepository bedtimeSchedules;
    @Mock EntitlementResolver entitlements;
    @Mock ImageUrlResolver images;
    @InjectMocks DashboardService svc;

    private CurrentUser user() {
        return new CurrentUser("u1", UserRole.USER);
    }

    private ChildProfile child(String id, String name) {
        ChildProfile p = new ChildProfile();
        p.setId(id); p.setUserId("u1"); p.setName(name); p.setPreferredLanguage("uk");
        return p;
    }

    private Story story(String id, String childId, String title) {
        Story s = new Story();
        s.setId(id); s.setUserId("u1"); s.setChildProfileId(childId);
        s.setTitle(title); s.setTheme("th");
        s.setCharacters(List.of("c")); s.setAgeGroup("6-8");
        s.setLength("short"); s.setLanguage("uk"); s.setContent("body");
        return s;
    }

    @Test
    void empty_user_returns_zeros_and_empty_lists() {
        when(stories.countByUserId("u1")).thenReturn(0L);
        when(stories.countByUserIdAndCreatedAtAfter(eq("u1"), any())).thenReturn(0L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(entitlements.isPro("u1")).thenReturn(false);

        DashboardDto dto = svc.getDashboard(user()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.aggregates().talesTotal()).isEqualTo(0);
        assertThat(dto.aggregates().talesThisWeek()).isEqualTo(0);
        assertThat(dto.aggregates().talesThisMonth()).isEqualTo(0);
        assertThat(dto.children()).isEmpty();
        assertThat(dto.recentTales()).isEmpty();
        assertThat(dto.isPro()).isFalse();
    }

    @Test
    void aggregates_total_is_passed_through() {
        when(stories.countByUserId("u1")).thenReturn(54L);
        when(stories.countByUserIdAndCreatedAtAfter(eq("u1"), any())).thenReturn(11L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(entitlements.isPro("u1")).thenReturn(true);

        DashboardDto dto = svc.getDashboard(user()).block();

        assertThat(dto.aggregates().talesTotal()).isEqualTo(54);
        assertThat(dto.isPro()).isTrue();
    }

    @Test
    void per_child_summary_includes_latest_tale_and_bedtime() {
        ChildProfile c = child("p1", "Лія");
        Story s = story("s1", "p1", "Зачарований дракон");
        // createdAt is @CreationTimestamp-managed — no setter available; it remains null in unit tests

        BedtimeSchedule bed = new BedtimeSchedule();
        bed.setChildProfileId("p1");
        bed.setLastSentAt(Instant.parse("2026-05-26T17:30:00Z"));

        when(stories.countByUserId("u1")).thenReturn(1L);
        when(stories.countByUserIdAndCreatedAtAfter(eq("u1"), any())).thenReturn(1L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of(c));
        when(stories.countByChildProfileId("p1")).thenReturn(1L);
        when(stories.findFirstByChildProfileIdOrderByCreatedAtDesc("p1")).thenReturn(Optional.of(s));
        when(bedtimeSchedules.findByChildProfileId("p1")).thenReturn(Optional.of(bed));
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of(s));
        when(entitlements.isPro("u1")).thenReturn(true);

        DashboardDto dto = svc.getDashboard(user()).block();

        assertThat(dto.children()).hasSize(1);
        var summary = dto.children().get(0);
        assertThat(summary.name()).isEqualTo("Лія");
        assertThat(summary.taleCount()).isEqualTo(1);
        assertThat(summary.latestTale()).isNotNull();
        assertThat(summary.latestTale().title()).isEqualTo("Зачарований дракон");
        // createdAt is @CreationTimestamp-managed (no setter); bedtime timestamp is what matters here
        assertThat(summary.lastBedtimeAt()).isEqualTo(Instant.parse("2026-05-26T17:30:00Z"));
    }

    @Test
    void per_child_summary_handles_missing_bedtime() {
        ChildProfile c = child("p1", "Артем");

        when(stories.countByUserId(anyString())).thenReturn(0L);
        when(stories.countByUserIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of(c));
        when(stories.countByChildProfileId("p1")).thenReturn(0L);
        when(stories.findFirstByChildProfileIdOrderByCreatedAtDesc("p1")).thenReturn(Optional.empty());
        when(bedtimeSchedules.findByChildProfileId("p1")).thenReturn(Optional.empty());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(entitlements.isPro("u1")).thenReturn(false);

        DashboardDto dto = svc.getDashboard(user()).block();

        var summary = dto.children().get(0);
        assertThat(summary.latestTale()).isNull();
        assertThat(summary.lastBedtimeAt()).isNull();
        assertThat(summary.taleCount()).isEqualTo(0);
    }

    @Test
    void recent_tales_are_passed_through_as_dto() {
        Story s = story("s1", null, "T");
        // createdAt is @CreationTimestamp-managed — no setter available

        when(stories.countByUserId(anyString())).thenReturn(1L);
        when(stories.countByUserIdAndCreatedAtAfter(anyString(), any())).thenReturn(1L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of(s));
        when(entitlements.isPro("u1")).thenReturn(true);

        DashboardDto dto = svc.getDashboard(user()).block();

        assertThat(dto.recentTales()).hasSize(1);
        assertThat(dto.recentTales().get(0).title()).isEqualTo("T");
    }
}
