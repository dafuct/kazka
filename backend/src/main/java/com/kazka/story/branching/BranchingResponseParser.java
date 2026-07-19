package com.kazka.story.branching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.story.branching.dto.BranchingChoice;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BranchingResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_FENCE = Pattern.compile("```(?:json)?\\s*(.+?)\\s*```", Pattern.DOTALL);

    // The whole choice block, wherever it sits in the response — with OR without a leading
    // "---" separator, and whether CHOICE_A / CHOICE_B are on separate lines or share one line.
    // Group 1 = choice A text (reluctant, stops at CHOICE_B); group 2 = choice B text (to end).
    // DOTALL (?s) lets group 1 span newlines; case-insensitive (?i) tolerates casing drift.
    private static final Pattern CHOICE_BLOCK = Pattern.compile(
            "(?is)CHOICE[_ ]?A\\s*[:\\-]\\s*(.+?)\\s*CHOICE[_ ]?B\\s*[:\\-]\\s*(.+?)\\s*$");

    // Any stray marker line — a "---" or "* * *" scene break, or a leftover CHOICE_ line — to
    // scrub from a body so separators/markers never reach the reader.
    private static final Pattern STRAY_MARKER =
            Pattern.compile("(?im)^[ \\t]*(?:-{3,}|\\*(?:[ \\t]*\\*)+|CHOICE[_ ]?[AB]\\s*[:\\-].*)[ \\t]*$");
    private static final Pattern TRAILING_SEPARATOR = Pattern.compile("(?s)\\s*\\n\\s*-{3,}\\s*$");

    // A prompt field echoed as a whole line ("Language: uk", "Theme: adventure") — drop the line.
    private static final Pattern LEAKED_FIELD_LINE = Pattern.compile(
            "(?imu)^[ \\t]*(?:language|theme|characters?|age|child\\s*name|approximate\\s*age)\\s*:.*$");
    // A language NAME the model uses as a label — either its own line ("Українська:") or a prefix
    // on the real prose ("Ukrainian: Матвій…"). Strip the label + colon; KEEP any prose after it
    // (removing the whole line would delete the story). (?u) folds Cyrillic case for the -i- match.
    private static final Pattern LEAKED_LANG = Pattern.compile(
            "(?imu)^[ \\t]*(?:ukrainian|english|українськ\\p{L}*|англійськ\\p{L}*)\\s*:[ \\t]*");

    public record Parsed(String body, List<BranchingChoice> choices) {}

    /** A body with an optional leading title line lifted out. */
    public record TitleBody(String title, String body) {}

    /** A fully-parsed branching segment: an optional title (opening only), the narrative body,
     *  and the choices (empty for the closing). */
    public record ParsedSegment(String title, String body, List<BranchingChoice> choices) {}

    /**
     * Parse a branching segment from the model's STRUCTURED-JSON reply
     * ({@code {"title","segment","choiceA","choiceB"}}). This is the primary path — a JSON field
     * can't be polluted by the stray labels/separators/lead-ins that free-form text parsing kept
     * leaking. If the reply isn't usable JSON, fall back to the regex text parser so behaviour is
     * never worse than before. The body is still run through {@link #cleanBody} as a safety net.
     *
     * @param expectChoices true for the opening/middle (two choices), false for the closing
     */
    public ParsedSegment parseJson(String raw, boolean expectChoices, String language) {
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode node = MAPPER.readTree(unwrapFences(raw.strip()));
                if (node.isObject() && node.hasNonNull("segment")) {
                    String body = cleanBody(node.path("segment").asText("").strip());
                    if (!body.isBlank()) {
                        String title = node.path("title").asText("").strip();
                        List<BranchingChoice> choices = List.of();
                        if (expectChoices) {
                            String a = node.path("choiceA").asText("").strip();
                            String b = node.path("choiceB").asText("").strip();
                            choices = (!a.isEmpty() && !b.isEmpty())
                                    ? List.of(new BranchingChoice("A", a), new BranchingChoice("B", b))
                                    : fallback(language);
                        }
                        return new ParsedSegment(title, body, choices);
                    }
                }
                log.warn("Branching JSON missing a usable 'segment'; falling back to text parse");
            } catch (Exception jsonException) {
                log.warn("Branching JSON parse failed ({}); falling back to text parse", jsonException.getMessage());
            }
        }
        // Fallback: reuse the regex text parser (never worse than the pre-JSON behaviour).
        if (expectChoices) {
            Parsed p = parse(raw, language);
            TitleBody tb = splitLeadingTitle(p.body());
            return new ParsedSegment(tb.title(), tb.body(), p.choices());
        }
        return new ParsedSegment("", parseFinal(raw).body(), List.of());
    }

    private static String unwrapFences(String raw) {
        Matcher matcher = JSON_FENCE.matcher(raw);
        return matcher.find() ? matcher.group(1) : raw;
    }

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
        scrubbed = LEAKED_FIELD_LINE.matcher(scrubbed).replaceAll("");
        scrubbed = LEAKED_LANG.matcher(scrubbed).replaceAll("");
        scrubbed = TRAILING_SEPARATOR.matcher(scrubbed).replaceAll("");
        // Removing a marker/label line leaves an empty line behind — collapse the resulting
        // run of blank lines back into a single paragraph break.
        scrubbed = scrubbed.replaceAll("\n{3,}", "\n\n");
        return scrubbed.strip();
    }

    private static List<BranchingChoice> fallback(String language) {
        boolean english = language != null && language.toLowerCase(Locale.ROOT).startsWith("en");
        return english
                ? List.of(new BranchingChoice("A", "Continue"), new BranchingChoice("B", "End the tale"))
                : List.of(new BranchingChoice("A", "Продовжити"), new BranchingChoice("B", "Завершити казку"));
    }
}
