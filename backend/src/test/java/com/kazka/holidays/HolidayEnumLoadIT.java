package com.kazka.holidays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class HolidayEnumLoadIT {

    @Test
    void all_holidays_load_uk_and_en_contexts() {
        for (Holiday h : Holiday.values()) {
            assertThat(h.label("uk")).as("UK label for " + h).isNotBlank();
            assertThat(h.label("en")).as("EN label for " + h).isNotBlank();
            assertThat(h.culturalContext("uk")).as("UK context for " + h)
                    .isNotBlank().hasSizeGreaterThan(100);
            assertThat(h.culturalContext("en")).as("EN context for " + h)
                    .isNotBlank().hasSizeGreaterThan(100);
        }
    }

    @Test
    void bilingual_language_resolves_to_uk_for_labels_and_contexts() {
        Holiday h = Holiday.CHRISTMAS;
        assertThat(h.label("bilingual")).isEqualTo(h.label("uk"));
        assertThat(h.culturalContext("bilingual")).isEqualTo(h.culturalContext("uk"));
    }

    @Test
    void unknown_language_falls_back_to_uk() {
        Holiday h = Holiday.EASTER;
        assertThat(h.label("xx")).isEqualTo(h.label("uk"));
    }
}
