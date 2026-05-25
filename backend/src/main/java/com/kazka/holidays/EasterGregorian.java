package com.kazka.holidays;

import java.time.LocalDate;

/**
 * Meeus's Gregorian Easter algorithm — integer math only, works for all years 1583+.
 * The Ukrainian Orthodox Church of Ukraine switched to the Gregorian computus in 2023.
 *
 * Reference: Jean Meeus, "Astronomical Algorithms" (1991), chapter 8.
 */
public record EasterGregorian() implements HolidayDateRule {
    @Override
    public LocalDate computeFor(int year) {
        int g = year % 19;
        int c = year / 100;
        int h = (c - c / 4 - (8 * c + 13) / 25 + 19 * g + 15) % 30;
        int i = h - (h / 28) * (1 - (29 / (h + 1)) * ((21 - g) / 11));
        int j = (year + year / 4 + i + 2 - c + c / 4) % 7;
        int l = i - j;
        int month = 3 + (l + 40) / 44;
        int day = l + 28 - 31 * (month / 4);
        return LocalDate.of(year, month, day);
    }
}
