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

    // The model sometimes echoes the prompt's labelled fields (e.g. "Language: uk" → "Українська:")
    // as a standalone line in the tale. These are meta, never narrative — scrub them out.
    // (?u) = UNICODE_CASE so the case-insensitive match also folds Cyrillic ("Українська" == "українська").
    private static final Pattern LEAKED_LABEL = Pattern.compile(
            "(?imu)^[ \\t]*(?:language|theme|characters?|age|child\\s*name|approximate\\s*age"
                    + "|українськ\\p{L}*|англійськ\\p{L}*|english)\\s*:.*$");

    public record Parsed(String body, List<BranchingChoice> choices) {}

    /** A body with an optional leading title line lifted out. */
    public record TitleBody(String title, String body) {}

    /**
     * Split a leading "book title" line off an opening body, mirroring the linear flow's rule
     * (a short, punctuation-free line of ≤6 words followed by real body). The branching opening
     * prompt asks the model to start with a title; this lifts it out so it isn't rendered inside
     * the tale. If the first line isn't title-shaped (e.g. it's already prose), nothing is stripped.
     */
    public static TitleBody splitLeadingTitle(String body) {
        if (body == null || body.isBlank()) return new TitleBody("", "");
        String[] parts = body.strip().split("\n", 2);
        String first = parts[0].strip();
        String rest = parts.length > 1 ? parts[1].strip() : "";
        if (!rest.isEmpty() && looksLikeTitle(first)) {
            return new TitleBody(first, rest);
        }
        return new TitleBody("", body.strip());
    }

    private static boolean looksLikeTitle(String line) {
        if (line.isEmpty() || line.length() > 60) return false;
        if (line.contains(". ") || line.contains("! ") || line.contains("? ")) return false;
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?")) return false;
        return line.split("\\s+").length <= 6;
    }

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
        scrubbed = LEAKED_LABEL.matcher(scrubbed).replaceAll("");
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
