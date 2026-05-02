package com.kazka.story;

import com.kazka.story.dto.GenerationRequest;
import org.junit.jupiter.api.Test;

import com.kazka.story.Story;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @Test
    void buildStorySystem_containsStructureGuidance() {
        String system = builder.buildStorySystem();

        assertThat(system).contains("Opening");
        assertThat(system).contains("Challenge");
        assertThat(system).contains("Resolution");
    }

    @Test
    void buildStoryUserMessage_uk_containsThemeAndCharacters() {
        GenerationRequest req = new GenerationRequest(
                "пригоди в лісі", List.of("Мія", "лисичка"), "6-8", "medium", "uk");

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
                "forest adventure", List.of("Mia"), "3-5", "short", "en");

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
    void buildSvgSystem_containsSvgOutputRules() {
        String system = builder.buildSvgSystem();

        assertThat(system).contains("viewBox");
        assertThat(system).contains("800");
        assertThat(system).contains("filter");
    }

    @Test
    void buildSvgUser_fillsSceneCharacterAndAgeGroup() {
        Story story = new Story();
        story.setCharacters(List.of("Mia", "the Fox"));
        story.setAgeGroup("6-8");
        story.setContent("Once there was a girl. She walked into the forest. More text here.");

        String user = builder.buildSvgUser(story, "a girl standing near a tall oak tree");

        assertThat(user).contains("a girl standing near a tall oak tree");
        assertThat(user).contains("Mia");
        assertThat(user).contains("the Fox");
        assertThat(user).contains("6-8");
        assertThat(user).contains("Once there was a girl");
        assertThat(user).contains("She walked into the forest");
    }
}
