package com.kazka.story;

import com.kazka.story.dto.GenerationRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PromptBuilder {

    private static final Map<String, Integer> LENGTH_WORDS = Map.of(
            "short", 300,
            "medium", 600,
            "long", 1000
    );

    private final String storySystem;
    private final String storyFewshotUk;
    private final String storyFewshotEn;
    private final String editorUk;
    private final String editorEn;
    private final String sceneExtractionSystem;
    private final Map<String, Map<Theme, String>> imageStyleByAge;

    public PromptBuilder() {
        this.storySystem = readResource("prompts/story-system.txt");
        this.storyFewshotUk = readResource("prompts/story-fewshot-uk.txt");
        this.storyFewshotEn = readResource("prompts/story-fewshot-en.txt");
        this.editorUk = readResource("prompts/editor-uk.txt");
        this.editorEn = readResource("prompts/editor-en.txt");
        this.sceneExtractionSystem = readResource("prompts/scene-extraction-system.txt");
        this.imageStyleByAge = Map.of(
                "3-5",  Map.of(
                        Theme.LIGHT, readResource("prompts/image-style-3-5-light.txt"),
                        Theme.DARK,  readResource("prompts/image-style-3-5-dark.txt")),
                "6-8",  Map.of(
                        Theme.LIGHT, readResource("prompts/image-style-6-8-light.txt"),
                        Theme.DARK,  readResource("prompts/image-style-6-8-dark.txt")),
                "9-12", Map.of(
                        Theme.LIGHT, readResource("prompts/image-style-9-12-light.txt"),
                        Theme.DARK,  readResource("prompts/image-style-9-12-dark.txt"))
        );
    }

    public String buildStorySystem(String language) {
        String fewshot = "uk".equals(language) ? storyFewshotUk : storyFewshotEn;
        return storySystem.strip() + "\n\n---\n\n" + fewshot.strip();
    }

    public String buildStoryUserMessage(GenerationRequest req) {
        int words = LENGTH_WORDS.getOrDefault(req.length(), 600);
        String characters = String.join(", ", req.characters());

        return "Write a fairy tale with the following parameters:\n\n" +
                "Language: " + req.language() + "\n" +
                "Theme: " + req.theme() + "\n" +
                "Characters: " + characters + "\n" +
                "Age: " + req.ageGroup() + "\n" +
                "Length: " + req.length() + " (~" + words + " words)\n\n" +
                "Age guidelines:\n" +
                "  3–5 → very short sentences, gentle repetition, familiar home/forest world\n" +
                "  6–8 → light adventure, clear moral, lively dialogue\n" +
                "  9–12 → richer plot, character growth, subtle lesson";
    }

    public String buildEditorSystem(String language) {
        return "uk".equals(language) ? editorUk.strip() : editorEn.strip();
    }

    public String buildSceneExtractionSystem() {
        return sceneExtractionSystem.strip();
    }

    public String buildSceneExtractionUser(String storyContent) {
        return "Story:\n\n" + (storyContent == null ? "" : storyContent);
    }

    public String buildImageStylePreamble(String ageGroup, Theme theme) {
        Map<Theme, String> byTheme = imageStyleByAge.getOrDefault(ageGroup, imageStyleByAge.get("6-8"));
        return byTheme.get(theme).strip();
    }

    public String buildImagePrompt(Story story, String scene, Theme theme) {
        String style = buildImageStylePreamble(story.getAgeGroup(), theme);
        String safeScene = (scene == null || scene.isBlank()) ? "" : " " + scene.strip();
        return style + safeScene;
    }

    private static String readResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read prompt file: " + path, e);
        }
    }
}
