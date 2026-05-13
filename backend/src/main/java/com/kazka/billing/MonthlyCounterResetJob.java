package com.kazka.billing;

import com.kazka.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class MonthlyCounterResetJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyCounterResetJob.class);

    private final UserRepository users;

    public MonthlyCounterResetJob(UserRepository users) {
        this.users = users;
    }

    /** Runs daily at 00:05 UTC; only mutates rows where counter_reset_at is in a previous month. */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void resetCounters() {
        Instant cutoff = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        int n = users.resetCountersUpdatedBefore(cutoff);
        if (n > 0) log.info("Reset stories_this_month for {} users (cutoff={})", n, cutoff);
    }
}
