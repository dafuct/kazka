package com.kazka.holidays;

import java.time.LocalDate;

public sealed interface HolidayDateRule
        permits FixedDate, EasterGregorian, NthWeekdayOfMonth {
    LocalDate computeFor(int year);
}
