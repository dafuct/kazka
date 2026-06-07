package com.kazka.story;

import com.kazka.child.Character;
import com.kazka.child.ChildProfile;
import com.kazka.story.dto.GenerationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderChildContextTest {

    @Test
    void should_inject_age_band_hint_for_3_year_old() {
        PromptBuilder pb = new PromptBuilder();
        var req = new GenerationRequest("theme", List.of("dog"), "3-5", "short", "uk", "p1", List.of());
        ChildProfile profile = new ChildProfile();
        profile.setName("Лія"); profile.setBirthYear((short) 2023);
        String msg = pb.buildStoryUserMessage(req, profile, List.of());
        assertThat(msg).contains("Child name: Лія").contains("Approximate age: 3");
    }

    @Test
    void should_inject_cast_block_when_characters_provided() {
        PromptBuilder pb = new PromptBuilder();
        var req = new GenerationRequest("theme", List.of("dog"), "6-8", "medium", "uk", "p1", List.of("c1"));
        ChildProfile profile = new ChildProfile(); profile.setName("X");
        com.kazka.child.Character murka = new com.kazka.child.Character();
        murka.setName("Мурка"); murka.setKind("animal");
        murka.setDescription("a tortoiseshell cat with green eyes");
        murka.setTraits(List.of("curious", "brave"));
        String msg = pb.buildStoryUserMessage(req, profile, List.of(murka));
        assertThat(msg).contains("RECURRING CAST")
                .contains("Мурка")
                .contains("curious, brave");
    }

    @Test
    void should_treat_bilingual_preference_as_uk_for_now() {
        PromptBuilder pb = new PromptBuilder();
        ChildProfile profile = new ChildProfile(); profile.setName("X"); profile.setPreferredLanguage("bilingual");
        String resolved = pb.resolveLanguage(profile, "uk");
        assertThat(resolved).isEqualTo("uk");
    }
}
