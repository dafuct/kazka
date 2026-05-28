package com.kazka.story.branching;

import com.kazka.story.branching.dto.BranchingChoice;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BranchingResponseParser {

    private static final Pattern SEPARATOR = Pattern.compile("\\s*\\n\\s*-{3,}\\s*\\n\\s*");
    private static final Pattern CHOICE_LINE = Pattern.compile("(?im)^\\s*CHOICE_([AB])\\s*:\\s*(.+?)\\s*$");

    private static final List<BranchingChoice> FALLBACK_CHOICES = List.of(
            new BranchingChoice("A", "Continue"),
            new BranchingChoice("B", "End the tale"));

    public record Parsed(String body, List<BranchingChoice> choices) {}

    public Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) {
            log.warn("Branching parser received empty response; using fallback");
            return new Parsed("", FALLBACK_CHOICES);
        }
        String text = raw.strip();

        Matcher sepMatcher = SEPARATOR.matcher(text);
        if (!sepMatcher.find()) {
            log.warn("No separator in branching response; using fallback choices");
            return new Parsed(text, FALLBACK_CHOICES);
        }

        String body = text.substring(0, sepMatcher.start()).strip();
        String tail = text.substring(sepMatcher.end());

        Matcher cm = CHOICE_LINE.matcher(tail);
        String a = null, b = null;
        while (cm.find()) {
            String letter = cm.group(1).toUpperCase();
            String txt = cm.group(2).strip();
            if ("A".equals(letter)) a = txt;
            else if ("B".equals(letter)) b = txt;
        }
        if (a == null || b == null) {
            log.warn("Missing CHOICE_A or CHOICE_B in branching response; using fallback");
            return new Parsed(body, FALLBACK_CHOICES);
        }
        return new Parsed(body, List.of(
                new BranchingChoice("A", a),
                new BranchingChoice("B", b)));
    }

    public Parsed parseFinal(String raw) {
        if (raw == null) return new Parsed("", List.of());
        return new Parsed(raw.strip(), List.of());
    }
}
