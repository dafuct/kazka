package com.kazka.holidays;

import java.time.LocalDate;
import java.time.Month;

public record FixedDate(Month month, int dayOfMonth) implements HolidayDateRule {
    @Override
    public LocalDate computeFor(int year) {
        return LocalDate.of(year, month, dayOfMonth);
    }
}
