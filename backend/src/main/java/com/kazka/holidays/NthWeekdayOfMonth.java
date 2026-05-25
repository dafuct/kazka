package com.kazka.holidays;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

public record NthWeekdayOfMonth(Month month, DayOfWeek dow, int nth) implements HolidayDateRule {
    @Override
    public LocalDate computeFor(int year) {
        // 1st day of month → walk forward to the nth occurrence of dow.
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        LocalDate firstMatch = firstOfMonth.with(TemporalAdjusters.dayOfWeekInMonth(1, dow));
        return firstMatch.plusWeeks(nth - 1L);
    }
}
