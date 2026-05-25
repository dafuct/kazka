package com.kazka.holidays;

import java.time.LocalDate;

public record EasterGregorian() implements HolidayDateRule {
    @Override public LocalDate computeFor(int year) {
        throw new UnsupportedOperationException("Implemented in Task 6");
    }
}
