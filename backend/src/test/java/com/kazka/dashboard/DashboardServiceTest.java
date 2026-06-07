package com.kazka.dashboard;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.child.bedtime.BedtimeSchedule;
import com.kazka.child.bedtime.BedtimeScheduleRepository;
import com.kazka.comics.StoryPanelRepository;
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
    @Mock ImageUrlResolver images;
    @Mock StoryPanelRepository panelRepository;
    @InjectMocks DashboardService svc;

    private CurrentUser user() {
        return new CurrentUser("u1", UserRole.USER);
    }

    private ChildProfile child(String id, String name) {
        ChildProfile profile = new ChildProfile();
        profile.setId(id); profile.setUserId("u1"); profile.setName(name); profile.setPreferredLanguage("uk");
        return profile;
    }

    private Story story(String id, String childId, String title) {
        Story story = new Story();
        story.setId(id); story.setUserId("u1"); story.setChildProfileId(childId);
        story.setTitle(title); story.setTheme("th");
        story.setCharacters(List.of("c")); story.setAgeGroup("6-8");
        story.setLength("short"); story.setLanguage("uk"); story.setContent("body");
        return story;
    }

    @Test
    void empty_user_returns_zeros_and_empty_lists() {
        when(stories.countByUserId("u1")).thenReturn(0L);
        when(stories.countByUserIdAndCreatedAtAfter(eq("u1"), any())).thenReturn(0L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());

        DashboardDto dto = svc.getDashboard(user()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.aggregates().talesTotal()).isEqualTo(0);
        assertThat(dto.aggregates().talesThisWeek()).isEqualTo(0);
        assertThat(dto.aggregates().talesThisMonth()).isEqualTo(0);
        assertThat(dto.children()).isEmpty();
        assertThat(dto.recentTales()).isEmpty();
    }

    @Test
    void aggregates_total_is_passed_through() {
        when(stories.countByUserId("u1")).thenReturn(54L);
        when(stories.countByUserIdAndCreatedAtAfter(eq("u1"), any())).thenReturn(11L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());

        DashboardDto dto = svc.getDashboard(user()).block();

        assertThat(dto.aggregates().talesTotal()).isEqualTo(54);
    }

    @Test
    void per_child_summary_includes_latest_tale_and_bedtime() {
        ChildProfile childProfile = child("p1", "Лія");
        Story childStory = story("s1", "p1", "Зачарований дракон");
        // createdAt is @CreationTimestamp-managed — no setter available; it remains null in unit tests

        BedtimeSchedule bed = new BedtimeSchedule();
        bed.setChildProfileId("p1");
        bed.setLastSentAt(Instant.parse("2026-05-26T17:30:00Z"));

        when(stories.countByUserId("u1")).thenReturn(1L);
        when(stories.countByUserIdAndCreatedAtAfter(eq("u1"), any())).thenReturn(1L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of(childProfile));
        when(stories.countByChildProfileId("p1")).thenReturn(1L);
        when(stories.findFirstByChildProfileIdOrderByCreatedAtDesc("p1")).thenReturn(Optional.of(childStory));
        when(bedtimeSchedules.findByChildProfileId("p1")).thenReturn(Optional.of(bed));
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of(childStory));

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
        ChildProfile childProfile2 = child("p1", "Артем");

        when(stories.countByUserId(anyString())).thenReturn(0L);
        when(stories.countByUserIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of(childProfile2));
        when(stories.countByChildProfileId("p1")).thenReturn(0L);
        when(stories.findFirstByChildProfileIdOrderByCreatedAtDesc("p1")).thenReturn(Optional.empty());
        when(bedtimeSchedules.findByChildProfileId("p1")).thenReturn(Optional.empty());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());

        DashboardDto dto = svc.getDashboard(user()).block();

        var summary = dto.children().get(0);
        assertThat(summary.latestTale()).isNull();
        assertThat(summary.lastBedtimeAt()).isNull();
        assertThat(summary.taleCount()).isEqualTo(0);
    }

    @Test
    void recent_tales_are_passed_through_as_dto() {
        Story recentStory = story("s1", null, "T");
        // createdAt is @CreationTimestamp-managed — no setter available

        when(stories.countByUserId(anyString())).thenReturn(1L);
        when(stories.countByUserIdAndCreatedAtAfter(anyString(), any())).thenReturn(1L);
        when(childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc("u1")).thenReturn(List.of());
        when(stories.findTop5ByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of(recentStory));

        DashboardDto dto = svc.getDashboard(user()).block();

        assertThat(dto.recentTales()).hasSize(1);
        assertThat(dto.recentTales().get(0).title()).isEqualTo("T");
    }
}
