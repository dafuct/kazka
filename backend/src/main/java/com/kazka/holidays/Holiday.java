package com.kazka.holidays;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Map;

public enum Holiday {
    ST_NICHOLAS("st-nicholas",
            Map.of("uk", "Святий Миколай", "en", "St Nicholas"),
            new FixedDate(Month.DECEMBER, 6), 1, 0),

    CHRISTMAS("christmas",
            Map.of("uk", "Різдво", "en", "Christmas"),
            new FixedDate(Month.DECEMBER, 25), 1, 1),

    MALANKA("malanka",
            Map.of("uk", "Маланка", "en", "Malanka"),
            new FixedDate(Month.JANUARY, 13), 0, 1),

    EASTER("easter",
            Map.of("uk", "Великдень", "en", "Easter"),
            new EasterGregorian(), 2, 2),

    VYSHYVANKA_DAY("vyshyvanka",
            Map.of("uk", "День вишиванки", "en", "Vyshyvanka Day"),
            new NthWeekdayOfMonth(Month.MAY, DayOfWeek.THURSDAY, 3), 0, 0),

    IVAN_KUPALA("ivan-kupala",
            Map.of("uk", "Івана Купала", "en", "Ivan Kupala"),
            new FixedDate(Month.JULY, 7), 1, 0),

    INDEPENDENCE_DAY("independence",
            Map.of("uk", "День Незалежності України", "en", "Independence Day"),
            new FixedDate(Month.AUGUST, 24), 0, 1);

    private final String id;
    private final Map<String, String> labels;
    private final HolidayDateRule dateRule;
    private final int daysBefore;
    private final int daysAfter;
    private final Map<String, String> contexts;

    Holiday(String id, Map<String, String> labels, HolidayDateRule dateRule, int daysBefore, int daysAfter) {
        this.id = id;
        this.labels = labels;
        this.dateRule = dateRule;
        this.daysBefore = daysBefore;
        this.daysAfter = daysAfter;
        this.contexts = Map.of(
                "uk", loadContext(id, "uk"),
                "en", loadContext(id, "en"));
    }

    private static String loadContext(String id, String lang) {
        try {
            return new ClassPathResource("prompts/holidays/" + id + "." + lang + ".md")
                    .getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException ioException) {
            throw new UncheckedIOException("Missing holiday context: " + id + "." + lang, ioException);
        }
    }

    public String id() { return id; }
    public HolidayDateRule dateRule() { return dateRule; }
    public int daysBefore() { return daysBefore; }
    public int daysAfter() { return daysAfter; }

    public String label(String lang) {
        return labels.getOrDefault(resolveLang(lang), labels.get("uk"));
    }

    public String culturalContext(String lang) {
        return contexts.getOrDefault(resolveLang(lang), contexts.get("uk"));
    }

    /** Bilingual / null → uk, matching Spec D's language resolution. */
    private static String resolveLang(String lang) {
        if (lang == null || "bilingual".equals(lang)) return "uk";
        return "en".equals(lang) ? "en" : "uk";
    }
}
