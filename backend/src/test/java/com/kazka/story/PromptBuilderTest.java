package com.kazka.story;

import com.kazka.story.dto.GenerationRequest;
import org.junit.jupiter.api.Test;

import com.kazka.story.Story;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @Test
    void buildStorySystem_uk_containsStructureAndUkrainianFewshot() {
        String system = builder.buildStorySystem("uk");

        assertThat(system).contains("Opening");
        assertThat(system).contains("Challenge");
        assertThat(system).contains("Resolution");
        assertThat(system).contains("STYLE EXAMPLES (Ukrainian)");
        assertThat(system).contains("Тимко");
    }

    @Test
    void buildStorySystem_en_containsStructureAndEnglishFewshot() {
        String system = builder.buildStorySystem("en");

        assertThat(system).contains("Opening");
        assertThat(system).contains("Resolution");
        assertThat(system).contains("STYLE EXAMPLES (English)");
        assertThat(system).contains("Pippin");
    }

    @Test
    void buildStoryUserMessage_uk_containsThemeAndCharacters() {
        GenerationRequest req = new GenerationRequest(
                "пригоди в лісі", List.of("Мія", "лисичка"), "6-8", "medium", "uk", "child-123", null);

        String user = builder.buildStoryUserMessage(req);

        assertThat(user).contains("пригоди в лісі");
        assertThat(user).contains("Мія");
        assertThat(user).contains("лисичка");
        assertThat(user).contains("6-8");
        assertThat(user).contains("Language: uk");
        assertThat(user).contains("600");
    }

    @Test
    void buildStoryUserMessage_en_containsLengthWords() {
        GenerationRequest req = new GenerationRequest(
                "forest adventure", List.of("Mia"), "3-5", "short", "en", "child-456", null);

        String user = builder.buildStoryUserMessage(req);

        assertThat(user).contains("forest adventure");
        assertThat(user).contains("Language: en");
        assertThat(user).contains("300");
    }

    @Test
    void buildEditorSystem_uk_containsUkrainianGrammarRules() {
        String system = builder.buildEditorSystem("uk");

        assertThat(system).contains("Відмінювання");
        assertThat(system).contains("Суржик");
    }

    @Test
    void buildEditorSystem_en_containsEnglishRules() {
        String system = builder.buildEditorSystem("en");

        assertThat(system).contains("Grammar errors");
        assertThat(system).contains("tense");
    }

    @Test
    void buildSceneExtractionSystem_returnsNonBlank() {
        assertThat(builder.buildSceneExtractionSystem()).isNotBlank();
    }

    @Test
    void buildSceneExtractionUser_wrapsStoryContent() {
        String user = builder.buildSceneExtractionUser("Once upon a time a fox ran.");

        assertThat(user).contains("Once upon a time a fox ran.");
    }

    @Test
    void buildImageStylePreamble_3_5_light_containsCrayonAndCream() {
        String style = builder.buildImageStylePreamble("3-5", Theme.LIGHT);
        assertThat(style).contains("4-year-old");
        assertThat(style).contains("crayons");
        assertThat(style).contains("cream paper");
    }

    @Test
    void buildImageStylePreamble_9_12_dark_containsPencilAndNavy() {
        String style = builder.buildImageStylePreamble("9-12", Theme.DARK);
        assertThat(style).contains("10-year-old");
        assertThat(style).contains("pencil");
        assertThat(style).contains("navy");
    }

    @Test
    void buildImageStylePreamble_unknownAge_fallsBackTo6to8() {
        String style = builder.buildImageStylePreamble("100-200", Theme.LIGHT);
        assertThat(style).contains("7-year-old");
    }

    @Test
    void buildImagePrompt_combinesStylePreambleAndScene() {
        Story story = new Story();
        story.setAgeGroup("6-8");
        story.setCharacters(List.of("Mia"));

        String prompt = builder.buildImagePrompt(story, "a fox under a tree", Theme.LIGHT);

        assertThat(prompt).contains("7-year-old");
        assertThat(prompt).contains("a fox under a tree");
    }

    @Test
    void buildImagePrompt_withNullScene_usesEmpty() {
        Story story = new Story();
        story.setAgeGroup("3-5");

        String prompt = builder.buildImagePrompt(story, null, Theme.DARK);

        assertThat(prompt).contains("4-year-old");
        assertThat(prompt).doesNotContain("null");
    }
}
