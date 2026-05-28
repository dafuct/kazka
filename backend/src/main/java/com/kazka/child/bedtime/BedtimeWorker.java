package com.kazka.child.bedtime;

import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.story.Story;
import com.kazka.story.StoryService;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class BedtimeWorker {

    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMinutes(15);

    private final BedtimeScheduleRepository scheduleRepo;
    private final ChildProfileRepository profiles;
    private final UserRepository users;
    private final EntitlementResolver entitlements;
    private final StoryService storyService;
    private final BedtimeMailer mailer;
    private final NextRunCalculator nextRunCalc;
    private final com.kazka.holidays.HolidayCalendar holidayCalendar;

    @Async
    public CompletableFuture<Void> enqueueAsync(String childProfileId) {
        run(childProfileId);
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void run(String childProfileId) {
        BedtimeSchedule s = scheduleRepo.findByChildProfileId(childProfileId).orElse(null);
        if (s == null || !s.isEnabled() || s.getFailedAt() != null) {
            log.debug("Bedtime skipped: schedule missing/disabled/failed for {}", childProfileId);
            return;
        }
        ChildProfile child = profiles.findById(childProfileId).orElse(null);
        if (child == null || child.getArchivedAt() != null) {
            log.debug("Bedtime skipped: child {} missing/archived", childProfileId);
            return;
        }
        User user = users.findById(child.getUserId()).orElse(null);
        if (user == null || user.getSuspendedAt() != null || !user.isEmailVerified()) {
            log.debug("Bedtime skipped: user unavailable for {}", childProfileId);
            return;
        }
        if (!entitlements.isPro(user.getId())) {
            log.info("Bedtime user {} no longer Pro — disabling schedule", user.getId());
            s.setEnabled(false);
            scheduleRepo.save(s);
            return;
        }
        if (alreadySentToday(s)) {
            log.debug("Bedtime dedup hit: schedule {} already sent today", childProfileId);
            return;
        }

        try {
            java.util.Optional<com.kazka.holidays.Holiday> holiday =
                    holidayCalendar.activeFor(java.time.Instant.now(), java.time.ZoneId.of(s.getTimezone()));

            String themeOverride = null;
            if (holiday.isPresent() && s.isHolidayThemesEnabled()) {
                com.kazka.holidays.Holiday h = holiday.get();
                String lang = child.getPreferredLanguage();
                themeOverride = h.label(lang) + "\n\n" + h.culturalContext(lang);
                log.info("Bedtime holiday active: {} for child {}", h.id(), child.getId());
            }

            Story story = storyService.generateForBedtime(child, s, user, themeOverride).block();
            mailer.send(Objects.requireNonNull(story), child, user);
            s.setLastSentAt(Instant.now());
            s.setRetryCount(0);
            s.setFailedAt(null);
            s.setNextRunAt(nextRunCalc.nextRun(
                    LocalTime.parse(s.getLocalTime()),
                    ZoneId.of(s.getTimezone()),
                    Instant.now()));
            scheduleRepo.save(s);
        } catch (Exception e) {
            log.warn("Bedtime generation/send failed for {}: {}", childProfileId, e.getMessage());
            int retries = s.getRetryCount() + 1;
            s.setRetryCount(retries);
            if (retries >= MAX_RETRIES) {
                s.setFailedAt(Instant.now());
                s.setNextRunAt(nextRunCalc.nextRun(
                        LocalTime.parse(s.getLocalTime()),
                        ZoneId.of(s.getTimezone()),
                        Instant.now()));
            } else {
                s.setNextRunAt(Instant.now().plus(RETRY_BACKOFF));
            }
            scheduleRepo.save(s);
        }
    }

    private boolean alreadySentToday(BedtimeSchedule s) {
        if (s.getLastSentAt() == null) return false;
        ZoneId tz = ZoneId.of(s.getTimezone());
        ZonedDateTime sentLocal = s.getLastSentAt().atZone(tz);
        ZonedDateTime nowLocal = Instant.now().atZone(tz);
        return sentLocal.toLocalDate().equals(nowLocal.toLocalDate());
    }
}
