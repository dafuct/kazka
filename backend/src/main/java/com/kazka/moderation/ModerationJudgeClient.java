package com.kazka.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ModerationJudgeClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, ModerationCategory> CODE_MAP = Map.ofEntries(
            Map.entry("S1", ModerationCategory.SEXUAL),
            Map.entry("S2", ModerationCategory.VIOLENCE),
            Map.entry("S3", ModerationCategory.HATE),
            Map.entry("S4", ModerationCategory.SELF_HARM),
            Map.entry("S5", ModerationCategory.DANGEROUS),
            Map.entry("S6", ModerationCategory.SUBSTANCE),
            Map.entry("S7", ModerationCategory.PROFANITY),
            Map.entry("S8", ModerationCategory.DEATH),
            Map.entry("S9", ModerationCategory.WAR));

    /**
     * Severity ranking — when the judge flags multiple categories, refuse with the
     * highest-severity match. Lower index = more severe.
     */
    private static final List<ModerationCategory> SEVERITY = List.of(
            ModerationCategory.SEXUAL,
            ModerationCategory.HATE,
            ModerationCategory.SELF_HARM,
            ModerationCategory.VIOLENCE,
            ModerationCategory.DANGEROUS,
            ModerationCategory.SUBSTANCE,
            ModerationCategory.WAR,
            ModerationCategory.DEATH,
            ModerationCategory.PROFANITY);

    private static final String POLICY = """
            Task: Check if there is unsafe content in the user message below according to our safety policy.

            The user content may be in Ukrainian or English; classify regardless of language.

            <BEGIN UNSAFE CONTENT CATEGORIES>
            S1: Sexual Content. Sex acts, nudity, sexualized content involving minors.
            S2: Violence. Graphic violence, gore, blood, body horror, torture, mutilation.
            S3: Hate. Slurs or attacks targeting any group.
            S4: Self-Harm. References to suicide, self-injury, or eating disorders.
            S5: Dangerous Instructions. Real-world unsafe activities (fire, weapons, poisons, dangerous animals).
            S6: Substances. Alcohol, drugs, tobacco.
            S7: Profanity or Crude Language. Explicit swear words or slurs, AND crude/vulgar
                words, scatological or toilet humour, bodily-function slang (pee, poop, fart and
                the like), insults or name-calling — anything coarse, rude, or otherwise
                inappropriate for a young child's bedtime story, in any language. Flag it even
                when it is only one word inside an otherwise innocent list of themes or characters.
            S8: Death. Any reference to death of a character — applicable to children's stories.
            S9: War. Soldiers, military, weapons of war, political conflict.
            <END UNSAFE CONTENT CATEGORIES>

            Output format — STRICT, no preamble, no explanation, no code fences:
            - First line: exactly the word 'safe' or 'unsafe' (lowercase, no punctuation).
            - If 'unsafe', second line: comma-separated category codes (e.g. 'S1' or 'S1,S3').
            - No other lines. No markdown.

            Examples:

            Input:
              Language: en
              Theme: three friendly bears explore a meadow
              Characters: Sofia, Bear
            Output:
            safe

            Input:
              Language: uk
              Theme: оголена принцеса
              Characters:
            Output:
            unsafe
            S1

            Input:
              Language: uk
              Theme: коні, зайчики, качка-пісячка, спорт
              Characters:
            Output:
            unsafe
            S7
            """;

    private final ModerationProperties props;
    private final WebClient webClient;

    public ModerationJudgeClient(ModerationProperties props, WebClient judgeWebClient) {
        this.props = props;
        this.webClient = judgeWebClient;
    }

    public ModerationResult classify(String language, String theme, List<String> characters) {
        String userBody = "Language: " + language + "\n"
                + "Theme: " + (theme == null ? "" : theme) + "\n"
                + "Characters: " + (characters == null ? "" : String.join(", ", characters));
        return classifyRaw(userBody);
    }

    public ModerationResult classifyScene(String language, String sceneText) {
        String userBody = "Language: " + language + "\n"
                + "Scene: " + (sceneText == null ? "" : sceneText);
        return classifyRaw(userBody);
    }

    private ModerationResult classifyRaw(String userBody) {
        try {
            // `reasoning_effort: "none"` disables Gemini 2.5's chain-of-thought for this call.
            // Without it, thinking consumes the entire 64-token budget before any visible
            // output is produced, so `message.content` comes back missing and the parser
            // returns JUDGE_UNAVAILABLE. Thinking adds zero quality for a 1-token "safe" /
            // "unsafe" verdict — keep it enabled only for creative paths (storyteller, editor).
            // Field is OpenAI-standard (used by o3-series too) so works against OpenAI /
            // OpenRouter as well; non-reasoning models ignore it. Bumped max_tokens 64 -> 128
            // as belt-and-suspenders in case a future model needs slightly more room.
            String responseBody = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", props.getJudgeModel(),
                            "messages", List.of(
                                    Map.of("role", "system", "content", POLICY),
                                    Map.of("role", "user", "content", userBody)),
                            "stream", false,
                            "max_tokens", 128,
                            "reasoning_effort", "none"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(props.getJudgeTimeout())
                    .block();

            if (responseBody == null) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
            JsonNode response = MAPPER.readTree(responseBody);
            String content = response.path("choices").path(0).path("message").path("content").asText("").trim();
            return parseGuardResponse(content);
        } catch (Exception exception) {
            log.warn("Moderation judge classification failed: {}", exception.getMessage());
            return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        }
    }

    private ModerationResult parseGuardResponse(String content) {
        if (content.isEmpty()) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        // Scan lines for a 'safe' / 'unsafe' token. Different judge models phrase the verdict
        // differently — Qwen sometimes prefixes "Assessment:", Gemini sometimes adds trailing
        // punctuation or wraps it in a phrase ("Verdict: safe", "**safe**", "safe."). Strip
        // backticks/asterisks/punctuation and check via word-boundary match. "unsafe" is checked
        // BEFORE "safe" because "safe" is a substring of "unsafe".
        String[] lines = content.split("\\r?\\n");
        int verdictIdx = -1;
        String verdict = null;
        for (int index = 0; index < lines.length; index++) {
            String cleaned = lines[index].toLowerCase()
                    .replaceAll("[`*_#>\"']", "")
                    .replaceAll("[.,:;!?]", " ")
                    .trim();
            if (cleaned.isEmpty()) continue;
            if (cleaned.matches(".*\\bunsafe\\b.*")) {
                verdictIdx = index; verdict = "unsafe"; break;
            }
            if (cleaned.matches(".*\\bsafe\\b.*")) {
                verdictIdx = index; verdict = "safe"; break;
            }
        }
        if (verdict == null) {
            log.warn("Judge response unparseable, treating as JUDGE_UNAVAILABLE (first 200 chars): {}",
                    content.substring(0, Math.min(200, content.length())));
            return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        }
        if ("safe".equals(verdict)) return ModerationResult.Allowed.INSTANCE;
        // unsafe — find the next non-blank line for category codes
        String codeLine = null;
        for (int index = verdictIdx + 1; index < lines.length; index++) {
            String trimmed = lines[index].trim();
            if (!trimmed.isEmpty()) { codeLine = trimmed; break; }
        }
        if (codeLine == null) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);

        String[] codes = codeLine.split(",");
        ModerationCategory chosen = null;
        int chosenSeverity = Integer.MAX_VALUE;
        for (String raw : codes) {
            ModerationCategory cat = CODE_MAP.get(raw.trim().toUpperCase());
            if (cat == null) continue;
            int sev = SEVERITY.indexOf(cat);
            if (sev >= 0 && sev < chosenSeverity) {
                chosen = cat;
                chosenSeverity = sev;
            }
        }
        if (chosen == null) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        return ModerationResult.Refused.of(chosen);
    }
}
