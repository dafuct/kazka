package com.kazka.dashboard;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.child.bedtime.BedtimeScheduleRepository;
import com.kazka.dashboard.dto.DashboardDto;
import com.kazka.illustration.ImageUrlResolver;
import com.kazka.story.StoryRepository;
import com.kazka.story.dto.StoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class DashboardService {

    private final StoryRepository stories;
    private final ChildProfileRepository childProfiles;
    private final BedtimeScheduleRepository bedtimeSchedules;
    private final EntitlementResolver entitlements;
    private final ImageUrlResolver images;

    @Transactional(readOnly = true)
    public Mono<DashboardDto> getDashboard(CurrentUser cu) {
        return Mono.fromCallable(() -> {
            String userId = cu.userId();
            Instant monthStart = computeMonthStart();
            Instant weekStart = computeWeekStart();

            long total = stories.countByUserId(userId);
            long thisMonth = stories.countByUserIdAndCreatedAtAfter(userId, monthStart);
            long thisWeek = stories.countByUserIdAndCreatedAtAfter(userId, weekStart);

            List<ChildProfile> profiles = childProfiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(userId);
            List<DashboardDto.ChildSummary> childSummaries = profiles.stream()
                    .map(this::summarize)
                    .toList();

            List<StoryDto> recent = stories.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
                    .map(s -> StoryDto.from(s, images))
                    .toList();

            boolean isPro = entitlements.isPro(userId);

            return new DashboardDto(
                    new DashboardDto.Aggregates(total, thisWeek, thisMonth),
                    childSummaries,
                    recent,
                    isPro);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private DashboardDto.ChildSummary summarize(ChildProfile c) {
        long count = stories.countByChildProfileId(c.getId());
        StoryDto latest = stories.findFirstByChildProfileIdOrderByCreatedAtDesc(c.getId())
                .map(s -> StoryDto.from(s, images))
                .orElse(null);
        Instant lastBedtimeAt = bedtimeSchedules.findByChildProfileId(c.getId())
                .map(b -> b.getLastSentAt())
                .orElse(null);
        return new DashboardDto.ChildSummary(c.getId(), c.getName(), count, latest, lastBedtimeAt);
    }

    private static Instant computeMonthStart() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Instant computeWeekStart() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        WeekFields wf = WeekFields.of(Locale.forLanguageTag("uk-UA"));
        LocalDate monday = today.with(wf.dayOfWeek(), 1);
        return monday.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
