package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IllustrationServiceTest {

    @Mock HuggingFaceClient hfClient;
    @Mock ImageStorageService imageStorage;
    @Mock StoryRepository storyRepo;
    @Mock PromptBuilder promptBuilder;

    @InjectMocks IllustrationService service;

    private Story sampleStory() {
        Story s = new Story();
        s.setId("s1");
        s.setTitle("The Fox");
        s.setAgeGroup("6-8");
        s.setCharacters(List.of("Mia"));
        s.setContent("Once upon a time...");
        return s;
    }

    @Test
    void generateAndStore_savesLightAndDarkPng_onSuccess() {
        Story story = sampleStory();
        when(storyRepo.findById("s1")).thenReturn(Optional.of(story));
        when(hfClient.generateText(any(), any())).thenReturn(Mono.just("a fox under a tree"));
        when(promptBuilder.buildSceneExtractionSystem()).thenReturn("scene-sys");
        when(promptBuilder.buildSceneExtractionUser(anyString())).thenReturn("scene-user");
        when(promptBuilder.buildImagePrompt(eq(story), anyString(), eq(Theme.LIGHT))).thenReturn("light-prompt");
        when(promptBuilder.buildImagePrompt(eq(story), anyString(), eq(Theme.DARK))).thenReturn("dark-prompt");
        when(hfClient.generateImage(eq("light-prompt"), eq(1024), eq(768))).thenReturn(Mono.just(new byte[]{1}));
        when(hfClient.generateImage(eq("dark-prompt"), eq(1024), eq(768))).thenReturn(Mono.just(new byte[]{2}));
        when(imageStorage.savePng("s1", Theme.LIGHT, new byte[]{1})).thenReturn("/uploads/s1-light.png");
        when(imageStorage.savePng("s1", Theme.DARK, new byte[]{2})).thenReturn("/uploads/s1-dark.png");

        StepVerifier.create(service.generateAndStore("s1")).verifyComplete();

        ArgumentCaptor<Story> saved = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).save(saved.capture());
        assertThat(saved.getValue().getIllustrationPathLight()).isEqualTo("/uploads/s1-light.png");
        assertThat(saved.getValue().getIllustrationPathDark()).isEqualTo("/uploads/s1-dark.png");
        assertThat(saved.getValue().getIllustrationStatus()).isEqualTo(IllustrationStatus.READY);
    }

    @Test
    void generateAndStore_marksFailed_whenImageCallFails() {
        Story story = sampleStory();
        when(storyRepo.findById("s1")).thenReturn(Optional.of(story));
        when(hfClient.generateText(any(), any())).thenReturn(Mono.just("a fox"));
        lenient().when(promptBuilder.buildSceneExtractionSystem()).thenReturn("scene-sys");
        lenient().when(promptBuilder.buildSceneExtractionUser(anyString())).thenReturn("scene-user");
        when(promptBuilder.buildImagePrompt(any(), anyString(), eq(Theme.LIGHT))).thenReturn("light-prompt");
        when(promptBuilder.buildImagePrompt(any(), anyString(), eq(Theme.DARK))).thenReturn("dark-prompt");
        when(hfClient.generateImage(eq("light-prompt"), eq(1024), eq(768)))
                .thenReturn(Mono.error(new RuntimeException("HF down")));
        lenient().when(hfClient.generateImage(eq("dark-prompt"), eq(1024), eq(768)))
                .thenReturn(Mono.just(new byte[]{2}));

        StepVerifier.create(service.generateAndStore("s1")).verifyComplete();

        ArgumentCaptor<Story> saved = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).save(saved.capture());
        assertThat(saved.getValue().getIllustrationStatus()).isEqualTo(IllustrationStatus.FAILED);
        assertThat(saved.getValue().getIllustrationPathLight()).isNull();
        assertThat(saved.getValue().getIllustrationPathDark()).isNull();
    }

    @Test
    void generateAndStore_noOp_whenStoryMissing() {
        when(storyRepo.findById("missing")).thenReturn(Optional.empty());

        StepVerifier.create(service.generateAndStore("missing")).verifyComplete();

        verify(storyRepo, org.mockito.Mockito.never()).save(any());
    }
}
