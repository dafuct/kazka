package com.kazka.holidays;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayCalendarTest {

    private final HolidayCalendar calc = new HolidayCalendar();

    private Instant localMidnight(LocalDate date, ZoneId tz) {
        return date.atStartOfDay(tz).toInstant();
    }

    @Test
    void should_return_christmas_on_dec_25() {
        Optional<Holiday> h = calc.activeFor(
                localMidnight(LocalDate.of(2026, 12, 25), ZoneId.of("Europe/Kyiv")),
                ZoneId.of("Europe/Kyiv"));
        assertThat(h).contains(Holiday.CHRISTMAS);
    }

    @Test
    void should_return_christmas_on_dec_24_eve() {
        // Christmas window: daysBefore=1, daysAfter=1 → Dec 24, 25, 26
        Optional<Holiday> h = calc.activeFor(
                localMidnight(LocalDate.of(2026, 12, 24), ZoneId.of("Europe/Kyiv")),
                ZoneId.of("Europe/Kyiv"));
        assertThat(h).contains(Holiday.CHRISTMAS);
    }

    @Test
    void should_return_empty_on_dec_23() {
        // One day before window-start — should NOT match
        Optional<Holiday> h = calc.activeFor(
                localMidnight(LocalDate.of(2026, 12, 23), ZoneId.of("Europe/Kyiv")),
                ZoneId.of("Europe/Kyiv"));
        assertThat(h).isEmpty();
    }

    @Test
    void should_return_empty_on_dec_27() {
        // One day after window-end
        Optional<Holiday> h = calc.activeFor(
                localMidnight(LocalDate.of(2026, 12, 27), ZoneId.of("Europe/Kyiv")),
                ZoneId.of("Europe/Kyiv"));
        assertThat(h).isEmpty();
    }

    @Test
    void should_return_st_nicholas_on_dec_5_eve() {
        // St Nicholas window: daysBefore=1, daysAfter=0 → Dec 5 and Dec 6 only
        Optional<Holiday> h = calc.activeFor(
                localMidnight(LocalDate.of(2026, 12, 5), ZoneId.of("Europe/Kyiv")),
                ZoneId.of("Europe/Kyiv"));
        assertThat(h).contains(Holiday.ST_NICHOLAS);
    }

    @Test
    void should_return_empty_on_dec_7() {
        // One day after St Nicholas (no daysAfter)
        Optional<Holiday> h = calc.activeFor(
                localMidnight(LocalDate.of(2026, 12, 7), ZoneId.of("Europe/Kyiv")),
                ZoneId.of("Europe/Kyiv"));
        assertThat(h).isEmpty();
    }

    @Test
    void should_return_easter_within_two_day_window() {
        // 2026 Easter is April 5; window ±2 → April 3..7
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2026, 4, 3), tz), tz))
                .contains(Holiday.EASTER);
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2026, 4, 7), tz), tz))
                .contains(Holiday.EASTER);
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2026, 4, 2), tz), tz))
                .isEmpty();
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2026, 4, 8), tz), tz))
                .isEmpty();
    }

    @Test
    void should_return_vyshyvanka_only_on_third_thursday_of_may() {
        // 2026: May 21 (Thursday). Window 0..0 → that day only.
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2026, 5, 21), tz), tz))
                .contains(Holiday.VYSHYVANKA_DAY);
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2026, 5, 20), tz), tz))
                .isEmpty();
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2026, 5, 22), tz), tz))
                .isEmpty();
    }

    @Test
    void should_not_return_malanka_on_dec_31() {
        // Year-boundary safety: Dec 31 is more than 1 day away from Jan 13 — no false match.
        ZoneId tz = ZoneId.of("Europe/Kyiv");
        assertThat(calc.activeFor(localMidnight(LocalDate.of(2025, 12, 31), tz), tz))
                .isEmpty();
    }
}
