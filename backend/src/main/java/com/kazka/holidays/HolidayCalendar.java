package com.kazka.holidays;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Component
public class HolidayCalendar {

    /**
     * Returns the active holiday for the given moment in the given timezone, or empty.
     * If two holidays overlap in window, the first in enum declaration order wins.
     * <p>
     * Scans year-1, year, and year+1 relative to {@code now} to handle year-boundary
     * windows correctly (e.g. Malanka on Jan 13 matched from a late-December check).
     */
    public Optional<Holiday> activeFor(Instant now, ZoneId childTz) {
        LocalDate today = now.atZone(childTz).toLocalDate();
        int[] candidateYears = { today.getYear() - 1, today.getYear(), today.getYear() + 1 };
        for (Holiday h : Holiday.values()) {
            for (int year : candidateYears) {
                LocalDate hday;
                try { hday = h.dateRule().computeFor(year); }
                catch (Exception e) { continue; }  // skip non-leap-year Feb 29 etc.
                LocalDate start = hday.minusDays(h.daysBefore());
                LocalDate end   = hday.plusDays(h.daysAfter());
                if (!today.isBefore(start) && !today.isAfter(end)) {
                    return Optional.of(h);
                }
            }
        }
        return Optional.empty();
    }
}
