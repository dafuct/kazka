package com.kazka.child.bedtime;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class NextRunCalculatorTest {

    private final NextRunCalculator calc = new NextRunCalculator();

    @Test
    void should_pick_today_when_local_time_still_in_future() {
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        Instant noonKyiv = Instant.parse("2026-06-15T09:00:00Z");   // 12:00 Kyiv (+03:00)
        Instant next = calc.nextRun(LocalTime.of(20, 30), tz, noonKyiv);
        assertThat(next).isEqualTo(Instant.parse("2026-06-15T17:30:00Z")); // 20:30 Kyiv same day
    }

    @Test
    void should_advance_to_tomorrow_when_local_time_has_passed_today() {
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        Instant lateEvening = Instant.parse("2026-06-15T18:00:00Z");   // 21:00 Kyiv, past 20:30
        Instant next = calc.nextRun(LocalTime.of(20, 30), tz, lateEvening);
        assertThat(next).isEqualTo(Instant.parse("2026-06-16T17:30:00Z")); // 20:30 Kyiv next day
    }

    @Test
    void should_handle_DST_spring_forward_in_Kyiv() {
        // Ukraine DST: last Sunday of March, 03:00 → 04:00 local (clock springs forward).
        // March 29, 2026 is the last Sunday.
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        Instant before = Instant.parse("2026-03-28T19:00:00Z");        // 21:00 Kyiv on 2026-03-28 (+02:00, pre-DST)
        Instant next = calc.nextRun(LocalTime.of(20, 30), tz, before);
        // Next 20:30 Kyiv is the 29th, post-transition (+03:00). 20:30 +03:00 = 17:30 UTC.
        assertThat(next).isEqualTo(Instant.parse("2026-03-29T17:30:00Z"));
    }

    @Test
    void should_handle_DST_fall_back_in_Kyiv() {
        // Ukraine fall-back: last Sunday of October, 04:00 → 03:00 local.
        // October 25, 2026 is the last Sunday. 20:30 local that day (+02:00) = 18:30 UTC.
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        Instant before = Instant.parse("2026-10-25T08:00:00Z");
        Instant next = calc.nextRun(LocalTime.of(20, 30), tz, before);
        assertThat(next).isEqualTo(Instant.parse("2026-10-25T18:30:00Z"));
    }

    @Test
    void should_work_in_diaspora_timezone_newyork() {
        ZoneId tz = ZoneId.of("America/New_York");
        Instant noonUtc = Instant.parse("2026-06-15T12:00:00Z");      // 08:00 NY (DST, -04:00)
        Instant next = calc.nextRun(LocalTime.of(20, 30), tz, noonUtc);
        assertThat(next).isEqualTo(Instant.parse("2026-06-16T00:30:00Z")); // 20:30 NY = 00:30 UTC next day
    }

    @Test
    void should_advance_to_tomorrow_when_after_equals_local_time_exactly() {
        // Boundary: if `after` is exactly the local time, advance to tomorrow
        // (rule: candidate must be strictly after `after`).
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        Instant exactly2030 = Instant.parse("2026-06-15T17:30:00Z");
        Instant next = calc.nextRun(LocalTime.of(20, 30), tz, exactly2030);
        assertThat(next).isEqualTo(Instant.parse("2026-06-16T17:30:00Z"));
    }
}
