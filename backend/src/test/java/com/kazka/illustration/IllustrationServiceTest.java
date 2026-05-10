package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.moderation.ModerationCategory;
import com.kazka.moderation.ModerationPipeline;
import com.kazka.moderation.ModerationProperties;
import com.kazka.moderation.ModerationResult;
import com.kazka.moderation.ModerationService;
import com.kazka.moderation.SuspensionService;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.Theme;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IllustrationServiceTest {

    @Mock HuggingFaceClient hfClient;
    @Mock ImageStorageService imageStorage;
    @Mock StoryRepository storyRepo;
    @Mock PromptBuilder promptBuilder;
    @Mock ModerationService moderationService;
    @Mock SuspensionService suspensionService;
    @Mock ModerationProperties modProps;

    @InjectMocks IllustrationService service;

    @BeforeEach
    void setUp() {
        // Allow all scenes by default so existing tests don't break when checkScene is called
        lenient().when(moderationService.checkScene(anyString(), anyString()))
                .thenReturn(ModerationResult.Allowed.INSTANCE);
        lenient().when(modProps.getSafeFallbackScene())
                .thenReturn("two friends in a sunlit forest at sunset");
        lenient().when(modProps.getJudgeModel())
                .thenReturn("Qwen/Qwen3-32B");
    }

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

    @Test
    void should_useSafeFallbackScene_when_moderationRefusesExtractedScene() {
        com.kazka.story.Story story = new com.kazka.story.Story();
        story.setId("story-1");
        story.setUserId("u1");
        story.setTitle("t");
        story.setLanguage("uk");
        story.setAgeGroup("6-8");
        story.setContent("body");
        when(storyRepo.findById("story-1")).thenReturn(java.util.Optional.of(story));

        String fallbackScene = "two friends in a sunlit forest at sunset";
        when(promptBuilder.buildSceneExtractionSystem()).thenReturn("scene-sys");
        when(promptBuilder.buildSceneExtractionUser(any())).thenReturn("scene-user");
        when(promptBuilder.buildImagePrompt(any(), eq(fallbackScene), eq(Theme.LIGHT)))
                .thenReturn("illustrated style: " + fallbackScene + " (light)");
        when(promptBuilder.buildImagePrompt(any(), eq(fallbackScene), eq(Theme.DARK)))
                .thenReturn("illustrated style: " + fallbackScene + " (dark)");
        when(hfClient.generateText(anyString(), anyString())).thenReturn(reactor.core.publisher.Mono.just("the witch with bloody hands"));
        when(moderationService.checkScene(eq("uk"), eq("the witch with bloody hands")))
                .thenReturn(com.kazka.moderation.ModerationResult.Refused.of(com.kazka.moderation.ModerationCategory.VIOLENCE));
        when(hfClient.generateImage(anyString(), eq(1024), eq(768)))
                .thenReturn(reactor.core.publisher.Mono.just(new byte[]{1, 2, 3}));
        lenient().when(imageStorage.savePng(anyString(), any(), any())).thenReturn("/uploads/fallback.png");

        service.generateAndStore("story-1").block();

        org.mockito.ArgumentCaptor<String> captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(hfClient, times(2)).generateImage(captor.capture(), eq(1024), eq(768));
        for (String prompt : captor.getAllValues()) {
            org.assertj.core.api.Assertions.assertThat(prompt).contains("two friends in a sunlit forest at sunset");
            org.assertj.core.api.Assertions.assertThat(prompt).doesNotContain("bloody");
        }
        verify(suspensionService).recordAndMaybeSuspend(
                eq("u1"),
                eq(com.kazka.moderation.ModerationPipeline.IMAGE_SCENE),
                eq(com.kazka.moderation.ModerationCategory.VIOLENCE),
                eq("uk"), anyString(), any(), anyString());
    }
}
