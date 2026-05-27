package com.kazka.story.branching;

import com.kazka.story.branching.dto.BranchingChoice;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        BranchingResponseParser.Parsed p = parser.parse(raw);

        assertThat(p.body()).isEqualTo("Oleh stepped into the forest. The trees whispered above him.");
        assertThat(p.choices()).containsExactly(
                new BranchingChoice("A", "Follow the firefly into the deeper woods"),
                new BranchingChoice("B", "Stay by the river and wait for the moon to rise"));
    }

    @Test
    void tolerates_extra_whitespace() {
        String raw = "  Body text here.  \n\n\n  ---  \n\n  CHOICE_A:    Option A text  \n  CHOICE_B:   Option B text  ";
        BranchingResponseParser.Parsed p = parser.parse(raw);
        assertThat(p.body()).isEqualTo("Body text here.");
        assertThat(p.choices()).extracting(BranchingChoice::text)
                .containsExactly("Option A text", "Option B text");
    }

    @Test
    void falls_back_when_separator_missing() {
        String raw = "Body text with no separator and no choices at all.";
        BranchingResponseParser.Parsed p = parser.parse(raw);
        assertThat(p.body()).isEqualTo("Body text with no separator and no choices at all.");
        assertThat(p.choices()).containsExactly(
                new BranchingChoice("A", "Continue"),
                new BranchingChoice("B", "End the tale"));
    }

    @Test
    void falls_back_when_only_one_choice_present() {
        String raw = """
                Body text.

                ---

                CHOICE_A: Only one option here
                """;
        BranchingResponseParser.Parsed p = parser.parse(raw);
        assertThat(p.body()).isEqualTo("Body text.");
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
}
