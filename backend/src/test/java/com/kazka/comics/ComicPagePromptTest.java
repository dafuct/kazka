package com.kazka.comics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComicPagePromptTest {

    private static Act beat(String scene, String speaker, String line) {
        return new Act(scene, "", List.of(new Act.Dialog(speaker, line)));
    }

    @Test
    void should_includeStyleLayoutAndScenes_when_composingPage() {
        List<Act> beats = List.of(
                beat("a small fox in a forest at dawn", "Лис", "Привіт"),
                beat("the fox crosses a river", "Лис", "Тримаюсь"),
                beat("the fox reaches a meadow", "Лис", "Як гарно"),
                beat("storm clouds gather", "Лис", "Ой"),
                beat("the fox is safe home at sunset", "Лис", "Нарешті"));

        String prompt = ComicPagePrompt.compose(beats, "uk");

        assertThat(prompt).contains("comic-book page").contains("white gutters");
        assertThat(prompt).contains("5 panels");
        assertThat(prompt).contains("a small fox in a forest at dawn")
                          .contains("storm clouds gather");
    }

    @Test
    void should_omitAllDialogAndText_when_composingPage() {
        // Image gen mangles non-Latin scripts and baked-in text can't be retranslated when the
        // reader switches language. The page must be wordless; dialog stays only in beats data.
        List<Act> beats = List.of(beat("a hero waves", "Хлопчик", "Привіт, друже!"));

        String prompt = ComicPagePrompt.compose(beats, "uk");

        assertThat(prompt)
                .doesNotContain("Привіт, друже!")
                .doesNotContain("Хлопчик")
                .doesNotContain("Speech bubble")
                .doesNotContain("Ukrainian");
        assertThat(prompt).contains("wordless").contains("no speech bubbles");
    }
}
