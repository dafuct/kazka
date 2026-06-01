package com.kazka.comics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.ai.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One Gemini call that turns a finished tale into {@value #BEATS} structured beats:
 * a visual scene description (art only) plus short dialog lines for speech bubbles.
 * The beats feed {@link ComicPagePrompt}, which composes a single comic-page prompt.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActsStructurer {

    static final int BEATS = 5;

    private final AiClient aiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern JSON_FENCE = Pattern.compile(
            "```(?:json)?\\s*(.+?)\\s*```", Pattern.DOTALL);

    public Mono<List<Act>> structure(String taleText, String language) {
        String system = systemPrompt(language);
        String user = "Tale:\n\n" + taleText;
        return aiClient.generateText(system, user)
                .map(this::parse);
    }

    private List<Act> parse(String raw) {
        String json = unwrapFences(raw).trim();
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Acts structurer returned invalid JSON: " + e.getMessage(), e);
        }
        if (!root.isArray()) {
            throw new IllegalStateException("Acts structurer: expected JSON array of beats, got "
                    + root.getNodeType() + ". Body head: " + truncate(raw));
        }
        if (root.size() != BEATS) {
            throw new IllegalStateException("Acts structurer: expected " + BEATS + " beats, got " + root.size());
        }
        List<Act> result = new ArrayList<>(BEATS);
        for (int i = 0; i < BEATS; i++) {
            try {
                result.add(mapper.treeToValue(root.get(i), Act.class));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("Acts structurer: beat " + (i + 1) + " malformed: "
                        + e.getMessage(), e);
            }
        }
        return result;
    }

    private static String unwrapFences(String s) {
        Matcher m = JSON_FENCE.matcher(s);
        return m.find() ? m.group(1) : s;
    }

    private static String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }

    private static String systemPrompt(String language) {
        String langName = "uk".equalsIgnoreCase(language) ? "Ukrainian" : "English";
        return """
            You are a comic-book editor. Split the children's fairy tale below into EXACTLY 5 beats in
            chronological order: (1) opening/setting, (2) first development, (3) climax/turning point,
            (4) second development, (5) resolution/closing.

            For each beat produce JSON with these fields:
              - "scene": a vivid one-sentence VISUAL description of this beat, in English, for an image
                generator. In beat 1, describe the protagonist's appearance concretely (species, size,
                colors, distinguishing features, outfit); later beats may refer to "the same character".
                Describe ART ONLY — do NOT request any text, words, signs, or lettering in the scene.
              - "narration": ONE short sentence of narration from the tale in %s. May be empty.
              - "dialog": a JSON array of {"speaker":"<name>","line":"<very short quote in %s>"} — at most
                2 entries per beat, each line at most 6 words (these become speech bubbles). May be empty.

            Return ONLY a JSON array of exactly 5 objects in the order above. No prose, no markdown fences.
            """.formatted(langName, langName);
    }
}
