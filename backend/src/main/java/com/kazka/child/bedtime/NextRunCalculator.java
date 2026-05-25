package com.kazka.child.bedtime;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Pure function: given a local bedtime and IANA timezone, returns the next UTC instant
 * matching that local time after a given reference instant.
 *
 * <p>DST semantics (Java {@link ZonedDateTime} defaults):
 * <ul>
 *   <li>Spring-forward gap (local time skipped) → resolves to next valid local time (forward).</li>
 *   <li>Fall-back overlap (local time happens twice) → resolves to the earlier offset.</li>
 * </ul>
 * Both are acceptable for bedtime scheduling.
 */
@Component
public class NextRunCalculator {

    public Instant nextRun(LocalTime localTime, ZoneId tz, Instant after) {
        ZonedDateTime nowInTz = after.atZone(tz);
        ZonedDateTime candidate = nowInTz
                .with(localTime)
                .withSecond(0)
                .withNano(0);
        if (!candidate.isAfter(nowInTz)) {
            candidate = candidate.plusDays(1);
        }
        return candidate.toInstant();
    }
}
