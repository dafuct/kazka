package com.kazka.story;

import com.kazka.story.dto.GenerationRequest;
import org.junit.jupiter.api.Test;

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

        assertThat(system).contains("відмінювання");
        assertThat(system).contains("суржик");
    }

    @Test
    void buildEditorSystem_en_containsEnglishRules() {
        String system = builder.buildEditorSystem("en");

        assertThat(system).contains("Grammar errors");
        assertThat(system).contains("tense");
    }

    @Test
    void buildIllustrationPrompt_includesTitleAndCharacters() {
        String prompt = builder.buildIllustrationPrompt("Мія та лисичка", List.of("Мія", "лисичка"));

        assertThat(prompt).contains("Мія та лисичка");
        assertThat(prompt).contains("Мія");
        assertThat(prompt).contains("лисичка");
        assertThat(prompt).contains("Watercolor");
    }
}
