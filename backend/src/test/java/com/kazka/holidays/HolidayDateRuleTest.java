package com.kazka.holidays;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayDateRuleTest {

    @Test
    void fixedDate_computes_same_date_every_year() {
        FixedDate dec25 = new FixedDate(Month.DECEMBER, 25);
        assertThat(dec25.computeFor(2024)).isEqualTo(LocalDate.of(2024, 12, 25));
        assertThat(dec25.computeFor(2025)).isEqualTo(LocalDate.of(2025, 12, 25));
        assertThat(dec25.computeFor(2030)).isEqualTo(LocalDate.of(2030, 12, 25));
    }

    @Test
    void fixedDate_handles_february_29_on_non_leap_year() {
        FixedDate feb29 = new FixedDate(Month.FEBRUARY, 29);
        // Leap year: works
        assertThat(feb29.computeFor(2024)).isEqualTo(LocalDate.of(2024, 2, 29));
        // Non-leap: throws DateTimeException
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> feb29.computeFor(2025))
                .isInstanceOf(java.time.DateTimeException.class);
    }
}
