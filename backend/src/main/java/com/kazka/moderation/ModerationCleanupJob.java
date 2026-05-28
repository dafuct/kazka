package com.kazka.moderation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModerationCleanupJob {

    private final FlaggedAttemptRepository flags;
    private final ModerationProperties props;

    @Scheduled(cron = "0 30 3 * * *")
    public void runScheduled() { runCleanup(); }

    @Transactional
    public long runCleanup() {
        Instant cutoff = Instant.now().minus(props.getRetentionDays(), ChronoUnit.DAYS);
        long deleted = flags.deleteByCreatedAtBefore(cutoff);
        log.info("Moderation cleanup deleted {} rows older than {}", deleted, cutoff);
        return deleted;
    }
}
