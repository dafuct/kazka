package com.kazka.holidays;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

public record NthWeekdayOfMonth(Month month, DayOfWeek dow, int nth) implements HolidayDateRule {
    @Override public LocalDate computeFor(int year) {
        throw new UnsupportedOperationException("Implemented in Task 5");
    }
}
