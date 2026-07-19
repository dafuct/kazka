package com.kazka.story.branching;

import com.kazka.story.branching.dto.BranchingChoice;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BranchingResponseParserTest {

    private final BranchingResponseParser parser = new BranchingResponseParser();

    @Test
    void parses_well_formed_response() {
        String raw = """
                Oleh stepped into the forest. The trees whispered above him.

                ---

                CHOICE_A: Follow the firefly into the deeper woods
                CHOICE_B: Stay by the river and wait for the moon to rise
                """;

        BranchingResponseParser.Parsed p = parser.parse(raw, "en");

        assertThat(p.body()).isEqualTo("Oleh stepped into the forest. The trees whispered above him.");
        assertThat(p.choices()).containsExactly(
                new BranchingChoice("A", "Follow the firefly into the deeper woods"),
                new BranchingChoice("B", "Stay by the river and wait for the moon to rise"));
    }

    @Test
    void tolerates_extra_whitespace() {
        String raw = "  Body text here.  \n\n\n  ---  \n\n  CHOICE_A:    Option A text  \n  CHOICE_B:   Option B text  ";
        BranchingResponseParser.Parsed p = parser.parse(raw, "en");
        assertThat(p.body()).isEqualTo("Body text here.");
        assertThat(p.choices()).extracting(BranchingChoice::text)
                .containsExactly("Option A text", "Option B text");
    }

    // Regression: the model returned no "---" separator AND put both choices on one line, so the
    // old parser fell back and baked "CHOICE_A: … CHOICE_B: …" straight into the tale body.
    @Test
    void extracts_choices_and_scrubs_markers_when_separator_missing() {
        String raw = "Опинившись у Місячному Саду, Матвій побачив квіти.\n\n"
                + "Що ж робити Матвієві?\n\n"
                + "CHOICE_A: Матвій вирішує підійти до Зайчика і запитати, що трапилось. "
                + "CHOICE_B: Матвій вирішує спочатку дослідити сад.";

        BranchingResponseParser.Parsed p = parser.parse(raw, "uk");

        assertThat(p.body())
                .doesNotContain("CHOICE_A", "CHOICE_B", "CHOICE_")
                .isEqualTo("Опинившись у Місячному Саду, Матвій побачив квіти.\n\nЩо ж робити Матвієві?");
        assertThat(p.choices()).containsExactly(
                new BranchingChoice("A", "Матвій вирішує підійти до Зайчика і запитати, що трапилось."),
                new BranchingChoice("B", "Матвій вирішує спочатку дослідити сад."));
    }

    @Test
    void parses_choices_that_share_a_single_line_with_separator() {
        String raw = "Body.\n\n---\n\nCHOICE_A: Go left CHOICE_B: Go right";
        BranchingResponseParser.Parsed p = parser.parse(raw, "en");
        assertThat(p.body()).isEqualTo("Body.");
        assertThat(p.choices()).extracting(BranchingChoice::text).containsExactly("Go left", "Go right");
    }

    @Test
    void falls_back_to_localized_choices_when_no_choices_present() {
        String raw = "Тіло казки без роздільника і без варіантів вибору.";
        BranchingResponseParser.Parsed p = parser.parse(raw, "uk");
        assertThat(p.body()).isEqualTo("Тіло казки без роздільника і без варіантів вибору.");
        assertThat(p.choices()).containsExactly(
                new BranchingChoice("A", "Продовжити"),
                new BranchingChoice("B", "Завершити казку"));
    }

    @Test
    void falls_back_and_scrubs_markers_when_only_one_choice_present() {
        String raw = """
                Body text.

                ---

                CHOICE_A: Only one option here
                """;
        BranchingResponseParser.Parsed p = parser.parse(raw, "en");
        assertThat(p.body()).isEqualTo("Body text.").doesNotContain("CHOICE_");
        assertThat(p.choices()).containsExactly(
                new BranchingChoice("A", "Continue"),
                new BranchingChoice("B", "End the tale"));
    }

    @Test
    void parses_segment_3_with_no_choice_block() {
        String raw = "And so Oleh returned home, smiling, and slept until morning.";
        BranchingResponseParser.Parsed p = parser.parseFinal(raw);
        assertThat(p.body()).isEqualTo("And so Oleh returned home, smiling, and slept until morning.");
        assertThat(p.choices()).isEmpty();
    }

    @Test
    void final_segment_scrubs_a_stray_choice_line() {
        String raw = "The tale ends happily.\n\n---\n\nCHOICE_A: leftover";
        BranchingResponseParser.Parsed p = parser.parseFinal(raw);
        assertThat(p.body()).isEqualTo("The tale ends happily.").doesNotContain("CHOICE_", "---");
        assertThat(p.choices()).isEmpty();
    }

    // Regression: the model echoed the "Language: uk" prompt field as a trailing "Українська:"
    // label into the opening (before the ---), so it leaked into the tale body.
    @Test
    void scrubs_a_leaked_language_label_from_the_body() {
        String raw = "Жив-був Матвійко, що катав машинку.\n\nУкраїнська:\n\n"
                + "---\n\nCHOICE_A: Піти в сад\nCHOICE_B: Лишитися вдома";
        BranchingResponseParser.Parsed p = parser.parse(raw, "uk");
        assertThat(p.body())
                .isEqualTo("Жив-був Матвійко, що катав машинку.")
                .doesNotContain("Українська");
        assertThat(p.choices()).extracting(BranchingChoice::text)
                .containsExactly("Піти в сад", "Лишитися вдома");
    }

    @Test
    void scrubs_leaked_field_labels_anywhere_in_the_body() {
        String raw = "Theme: adventure\n\nOnce upon a time a boy played.\n\nLanguage: en\n\n"
                + "---\n\nCHOICE_A: Go out\nCHOICE_B: Stay in";
        BranchingResponseParser.Parsed p = parser.parse(raw, "en");
        assertThat(p.body())
                .isEqualTo("Once upon a time a boy played.")
                .doesNotContain("Theme:", "Language:");
    }

    @Test
    void splitLeadingTitle_lifts_a_title_line() {
        BranchingResponseParser.TitleBody tb =
                BranchingResponseParser.splitLeadingTitle("Матвійко та Червона Машинка\n\nЖив-був Матвійко.");
        assertThat(tb.title()).isEqualTo("Матвійко та Червона Машинка");
        assertThat(tb.body()).isEqualTo("Жив-був Матвійко.");
    }

    @Test
    void splitLeadingTitle_leaves_prose_untouched() {
        // First line ends with a period → a sentence, not a title.
        BranchingResponseParser.TitleBody tb =
                BranchingResponseParser.splitLeadingTitle("Жив-був Матвійко.\n\nВін катав машинку.");
        assertThat(tb.title()).isEmpty();
        assertThat(tb.body()).isEqualTo("Жив-був Матвійко.\n\nВін катав машинку.");
    }

    @Test
    void splitLeadingTitle_does_not_strip_a_single_line_body() {
        BranchingResponseParser.TitleBody tb = BranchingResponseParser.splitLeadingTitle("Opening body");
        assertThat(tb.title()).isEmpty();
        assertThat(tb.body()).isEqualTo("Opening body");
    }
}
