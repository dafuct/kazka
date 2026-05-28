package com.kazka.child.bedtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class BedtimeSweepJob {

    private static final Duration HORIZON = Duration.ofHours(1);

    private final BedtimeScheduleRepository repo;
    private final BedtimeWorker worker;

    public BedtimeSweepJob(BedtimeScheduleRepository repo, BedtimeWorker worker) {
        this.repo = repo;
        this.worker = worker;
    }

    /** Runs every 5 minutes; horizon caps catch-up to the last hour. */
    @Scheduled(cron = "0 */5 * * * *", zone = "UTC")
    public void sweep() {
        runOnce();
    }

    /** Test-friendly entry point that returns the count of schedules picked up. */
    public int runOnce() {
        Instant now = Instant.now();
        Instant horizon = now.minus(HORIZON);
        List<BedtimeSchedule> due = repo.findDueForSweep(now, horizon);
        if (!due.isEmpty()) log.info("Bedtime sweep found {} due schedule(s)", due.size());
        for (BedtimeSchedule s : due) {
            worker.enqueueAsync(s.getChildProfileId());
        }
        return due.size();
    }
}
