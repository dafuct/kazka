package com.kazka.story;

import com.kazka.story.dto.GenerationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @Test
    void buildPrompt_uk_containsThemeAndCharacters() {
        GenerationRequest req = new GenerationRequest(
                "пригоди в лісі", List.of("Мія", "лисичка"), "6-8", "medium", "uk");

        String prompt = builder.buildPrompt(req);

        assertThat(prompt).contains("пригоди в лісі");
        assertThat(prompt).contains("Мія");
        assertThat(prompt).contains("лисичка");
        assertThat(prompt).contains("6-8");
        assertThat(prompt).contains("400");
    }

    @Test
    void buildPrompt_en_containsEnglishSystemText() {
        GenerationRequest req = new GenerationRequest(
                "forest adventure", List.of("Mia"), "3-5", "short", "en");

        String prompt = builder.buildPrompt(req);

        assertThat(prompt).contains("forest adventure");
        assertThat(prompt).contains("150");
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
