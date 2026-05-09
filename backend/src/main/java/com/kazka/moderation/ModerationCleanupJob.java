package com.kazka.moderation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class ModerationCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ModerationCleanupJob.class);

    private final FlaggedAttemptRepository flags;
    private final ModerationProperties props;

    public ModerationCleanupJob(FlaggedAttemptRepository flags, ModerationProperties props) {
        this.flags = flags;
        this.props = props;
    }

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
