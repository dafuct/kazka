package com.kazka.story.branching;

import com.kazka.story.branching.dto.BranchingChoice;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BranchingResponseParser {

    // The whole choice block, wherever it sits in the response — with OR without a leading
    // "---" separator, and whether CHOICE_A / CHOICE_B are on separate lines or share one line.
    // Group 1 = choice A text (reluctant, stops at CHOICE_B); group 2 = choice B text (to end).
    // DOTALL (?s) lets group 1 span newlines; case-insensitive (?i) tolerates casing drift.
    private static final Pattern CHOICE_BLOCK = Pattern.compile(
            "(?is)CHOICE[_ ]?A\\s*[:\\-]\\s*(.+?)\\s*CHOICE[_ ]?B\\s*[:\\-]\\s*(.+?)\\s*$");

    // Any stray marker line (a lone "---" separator or a leftover CHOICE_ line) to scrub from a
    // body when we cannot parse a clean pair — so markers never reach the reader.
    private static final Pattern STRAY_MARKER =
            Pattern.compile("(?im)^[ \\t]*(?:-{3,}|CHOICE[_ ]?[AB]\\s*[:\\-].*)[ \\t]*$");
    private static final Pattern TRAILING_SEPARATOR = Pattern.compile("(?s)\\s*\\n\\s*-{3,}\\s*$");

    public record Parsed(String body, List<BranchingChoice> choices) {}

    /**
     * Parse a mid-tale segment into its narrative body and the two branching choices.
     * The narrative body is guaranteed free of {@code CHOICE_A/CHOICE_B} markers and
     * {@code ---} separators, even when the model omits the expected format — the raw
     * markers used to leak into the tale otherwise. Falls back to generic (localized)
     * choices only when no clean A+B pair can be recovered.
     */
    public Parsed parse(String raw, String language) {
        if (raw == null || raw.isBlank()) {
            log.warn("Branching parser received empty response; using fallback choices");
            return new Parsed("", fallback(language));
        }
        String text = raw.strip();

        Matcher block = CHOICE_BLOCK.matcher(text);
        if (block.find()) {
            String choiceA = block.group(1).strip();
            String choiceB = block.group(2).strip();
            if (!choiceA.isEmpty() && !choiceB.isEmpty()) {
                String body = cleanBody(text.substring(0, block.start()));
                return new Parsed(body, List.of(
                        new BranchingChoice("A", choiceA),
                        new BranchingChoice("B", choiceB)));
            }
        }

        // No parseable A+B pair. Never leak markers into the narrative: scrub any stray
        // CHOICE_/--- lines out of the body and fall back to generic localized choices.
        log.warn("Branching response missing a clean CHOICE_A/CHOICE_B pair; scrubbing markers, using fallback choices");
        return new Parsed(cleanBody(text), fallback(language));
    }

    /** The closing segment carries no choice block, but the model sometimes appends a stray
     *  CHOICE / separator line anyway — scrub it so it never reaches the reader. */
    public Parsed parseFinal(String raw) {
        if (raw == null) return new Parsed("", List.of());
        return new Parsed(cleanBody(raw.strip()), List.of());
    }

    private String cleanBody(String body) {
        String scrubbed = STRAY_MARKER.matcher(body).replaceAll("");
        scrubbed = TRAILING_SEPARATOR.matcher(scrubbed).replaceAll("");
        return scrubbed.strip();
    }

    private static List<BranchingChoice> fallback(String language) {
        boolean english = language != null && language.toLowerCase(Locale.ROOT).startsWith("en");
        return english
                ? List.of(new BranchingChoice("A", "Continue"), new BranchingChoice("B", "End the tale"))
                : List.of(new BranchingChoice("A", "Продовжити"), new BranchingChoice("B", "Завершити казку"));
    }
}
