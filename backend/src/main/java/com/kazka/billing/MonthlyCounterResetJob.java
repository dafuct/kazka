package com.kazka.billing;

import com.kazka.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@RequiredArgsConstructor
@Component
public class MonthlyCounterResetJob {

    private final UserRepository users;

    /** Runs daily at 00:05 UTC; only mutates rows where counter_reset_at is in a previous month. */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void resetCounters() {
        Instant cutoff = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        int n = users.resetCountersUpdatedBefore(cutoff);
        if (n > 0) log.info("Reset stories_this_month for {} users (cutoff={})", n, cutoff);
    }
}
