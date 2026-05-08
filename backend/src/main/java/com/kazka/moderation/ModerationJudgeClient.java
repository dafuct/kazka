package com.kazka.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class ModerationJudgeClient {

    private static final Logger log = LoggerFactory.getLogger(ModerationJudgeClient.class);
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

            <BEGIN UNSAFE CONTENT CATEGORIES>
            S1: Sexual Content. Sex acts, nudity, sexualized content involving minors.
            S2: Violence. Graphic violence, gore, blood, body horror, torture, mutilation.
            S3: Hate. Slurs or attacks targeting any group.
            S4: Self-Harm. References to suicide, self-injury, or eating disorders.
            S5: Dangerous Instructions. Real-world unsafe activities (fire, weapons, poisons, dangerous animals).
            S6: Substances. Alcohol, drugs, tobacco.
            S7: Profanity. Explicit swear words or slurs.
            S8: Death. Any reference to death of a character — applicable to children's stories.
            S9: War. Soldiers, military, weapons of war, political conflict.
            <END UNSAFE CONTENT CATEGORIES>

            Provide your safety assessment for the user message:
            - First line must read 'safe' or 'unsafe'.
            - If unsafe, second line must be a comma-separated list of violated categories.
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
            String responseBody = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", props.getJudgeModel(),
                            "messages", List.of(
                                    Map.of("role", "system", "content", POLICY),
                                    Map.of("role", "user", "content", userBody)),
                            "stream", false,
                            "max_tokens", 64))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(props.getJudgeTimeout())
                    .block();

            if (responseBody == null) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
            JsonNode response = MAPPER.readTree(responseBody);
            String content = response.path("choices").path(0).path("message").path("content").asText("").trim();
            return parseGuardResponse(content);
        } catch (Exception e) {
            log.warn("Moderation judge classification failed: {}", e.getMessage());
            return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        }
    }

    private ModerationResult parseGuardResponse(String content) {
        if (content.isEmpty()) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        String[] lines = content.split("\\r?\\n", 2);
        String verdict = lines[0].trim().toLowerCase();
        if ("safe".equals(verdict)) return ModerationResult.Allowed.INSTANCE;
        if (!"unsafe".equals(verdict)) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);
        if (lines.length < 2) return ModerationResult.Refused.of(ModerationCategory.JUDGE_UNAVAILABLE);

        String[] codes = lines[1].split(",");
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
