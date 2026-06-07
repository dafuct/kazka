package com.kazka.child.bedtime;

import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.holidays.Holiday;
import com.kazka.holidays.HolidayCalendar;
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
import java.util.Optional;
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
    private final StoryService storyService;
    private final BedtimeMailer mailer;
    private final NextRunCalculator nextRunCalc;
    private final HolidayCalendar holidayCalendar;

    @Async
    public CompletableFuture<Void> enqueueAsync(String childProfileId) {
        run(childProfileId);
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void run(String childProfileId) {
        BedtimeSchedule schedule = scheduleRepo.findByChildProfileId(childProfileId).orElse(null);
        if (schedule == null || !schedule.isEnabled() || schedule.getFailedAt() != null) {
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
        if (alreadySentToday(schedule)) {
            log.debug("Bedtime dedup hit: schedule {} already sent today", childProfileId);
            return;
        }

        try {
            Optional<Holiday> holiday =
                    holidayCalendar.activeFor(Instant.now(), ZoneId.of(schedule.getTimezone()));

            String themeOverride = null;
            if (holiday.isPresent() && schedule.isHolidayThemesEnabled()) {
                Holiday activeHoliday = holiday.get();
                String lang = child.getPreferredLanguage();
                themeOverride = activeHoliday.label(lang) + "\n\n" + activeHoliday.culturalContext(lang);
                log.info("Bedtime holiday active: {} for child {}", activeHoliday.id(), child.getId());
            }

            Story story = storyService.generateForBedtime(child, schedule, user, themeOverride).block();
            mailer.send(Objects.requireNonNull(story), child, user);
            schedule.setLastSentAt(Instant.now());
            schedule.setRetryCount(0);
            schedule.setFailedAt(null);
            schedule.setNextRunAt(nextRunCalc.nextRun(
                    LocalTime.parse(schedule.getLocalTime()),
                    ZoneId.of(schedule.getTimezone()),
                    Instant.now()));
            scheduleRepo.save(schedule);
        } catch (Exception exception) {
            log.warn("Bedtime generation/send failed for {}: {}", childProfileId, exception.getMessage());
            int retries = schedule.getRetryCount() + 1;
            schedule.setRetryCount(retries);
            if (retries >= MAX_RETRIES) {
                schedule.setFailedAt(Instant.now());
                // Advance to tomorrow's bedtime so a permanent failure doesn't re-schedule
                // for the same bedtime window that just failed (which may be only minutes away).
                schedule.setNextRunAt(nextRunCalc.nextRun(
                        LocalTime.parse(schedule.getLocalTime()),
                        ZoneId.of(schedule.getTimezone()),
                        Instant.now().plus(Duration.ofDays(1))));
            } else {
                schedule.setNextRunAt(Instant.now().plus(RETRY_BACKOFF));
            }
            scheduleRepo.save(schedule);
        }
    }

    private boolean alreadySentToday(BedtimeSchedule schedule) {
        if (schedule.getLastSentAt() == null) return false;
        ZoneId tz = ZoneId.of(schedule.getTimezone());
        ZonedDateTime sentLocal = schedule.getLastSentAt().atZone(tz);
        ZonedDateTime nowLocal = Instant.now().atZone(tz);
        return sentLocal.toLocalDate().equals(nowLocal.toLocalDate());
    }
}
