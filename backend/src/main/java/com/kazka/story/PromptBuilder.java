package com.kazka.story;

import com.kazka.story.dto.GenerationRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    private static final Map<String, Integer> LENGTH_WORDS = Map.of(
            "short", 150,
            "medium", 400,
            "long", 800
    );

    private final String systemUk;
    private final String systemEn;
    private final String illustrationStyle;

    public PromptBuilder() {
        this.systemUk = readResource("prompts/system-uk.txt");
        this.systemEn = readResource("prompts/system-en.txt");
        this.illustrationStyle = readResource("prompts/illustration-style.txt").strip();
    }

    public String buildPrompt(GenerationRequest req) {
        String system = "uk".equals(req.language()) ? systemUk : systemEn;
        int words = LENGTH_WORDS.getOrDefault(req.length(), 400);
        String characters = String.join(", ", req.characters());

        return system.strip() + "\n\n" +
                "Theme: " + req.theme() + "\n" +
                "Characters: " + characters + "\n" +
                "Age group: " + req.ageGroup() + "\n" +
                "Target length: ~" + words + " words";
    }

    public String buildIllustrationPrompt(String title, List<String> characters) {
        String chars = String.join(", ", characters);
        return title + " featuring " + chars + ". " + illustrationStyle;
    }

    private static String readResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read prompt file: " + path, e);
        }
    }
}
