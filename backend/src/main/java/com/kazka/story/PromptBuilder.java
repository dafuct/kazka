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
            "short", 300,
            "medium", 600,
            "long", 1000
    );

    private final String storySystem;
    private final String editorUk;
    private final String editorEn;
    private final String sceneExtractionSystem;
    private final String svgSystem;
    private final Map<String, Map<Theme, String>> imageStyleByAge;

    public PromptBuilder() {
        this.storySystem = readResource("prompts/story-system.txt");
        this.editorUk = readResource("prompts/editor-uk.txt");
        this.editorEn = readResource("prompts/editor-en.txt");
        this.sceneExtractionSystem = readResource("prompts/scene-extraction-system.txt");
        this.svgSystem = readResource("prompts/svg-system.txt");
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

    public String buildStorySystem() {
        return storySystem.strip();
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

    public String buildSvgSystem() {
        return svgSystem.strip();
    }

    public String buildSvgUser(Story story, String sceneDescription) {
        String mainChar = (story.getCharacters() == null || story.getCharacters().isEmpty())
                ? "a child" : story.getCharacters().get(0);
        List<String> supporting = (story.getCharacters() != null && story.getCharacters().size() > 1)
                ? story.getCharacters().subList(1, story.getCharacters().size())
                : List.of();
        String supportingLine = supporting.isEmpty()
                ? ""
                : "Supporting characters: " + String.join(", ", supporting) + "\n";

        return "Generate a flat vector SVG illustration for a children's fairy tale.\n\n" +
               "Scene: " + sceneDescription + "\n" +
               "Main character: " + mainChar + "\n" +
               supportingLine +
               "Character position: center-left of canvas\n" +
               "Mood: warm, cheerful, safe\n" +
               "Time of day: sunny afternoon or golden hour\n" +
               "Sky color: soft blue (#a8d8f0) fading to peach (#fde8c8)\n" +
               "Ground color: soft green (#7bc67e)\n\n" +
               "Decorative elements (choose 2 maximum):\n" +
               "- A lollipop-style tree to the right of character\n" +
               "- 2-3 white clouds in upper sky\n" +
               "- Small colorful flowers in ground strip\n" +
               "- Sun in upper-left corner with short rays\n\n" +
               "Character details:\n" +
               "- Head shape: circle, choose an appropriate warm color for this character based on their name and type\n" +
               "- Eyes: two small dark circles with white highlight dot\n" +
               "- Expression: happy, slightly smiling (use a simple arc path for mouth)\n" +
               "- Body: rounded rectangle, same color family as head\n" +
               "- Scale: character height = 35-40% of canvas height (210-240px)\n\n" +
               "Age group: " + story.getAgeGroup() + "\n" +
               "Story context: " + firstTwoSentences(story.getContent());
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

    private static String firstTwoSentences(String content) {
        if (content == null || content.isBlank()) return "";
        String[] parts = content.split("(?<=\\.)\\s+", 3);
        return parts.length >= 2 ? parts[0] + " " + parts[1] : parts[0];
    }

    private static String readResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read prompt file: " + path, e);
        }
    }
}
