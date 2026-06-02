package com.kazka.comics;

import com.kazka.device.PushNotifier;
import com.kazka.illustration.ImageStorage;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComicsBuilderTest {

    @Mock ActsStructurer structurer;
    @Mock NanoBananaClient nanoBanana;
    @Mock ImageStorage imageStorage;
    @Mock StoryRepository storyRepository;
    @Mock StoryPanelRepository panelRepository;
    @Mock PushNotifier pushNotifier;

    ComicsBuilder builder;
    Story story;

    private static List<Act> fiveBeats() {
        return List.of(
                new Act("s1", "n1", List.of(new Act.Dialog("Fox", "Hi"))),
                new Act("s2", "n2", List.of()),
                new Act("s3", "n3", List.of()),
                new Act("s4", "n4", List.of()),
                new Act("s5", "n5", List.of()));
    }

    @BeforeEach
    void setUp() {
        builder = new ComicsBuilder(structurer, nanoBanana, imageStorage,
                storyRepository, panelRepository, pushNotifier, Duration.ofSeconds(60));
        story = new Story();
        story.setId("story-1");
        story.setUserId("user-1");
        story.setTitle("Test");
        story.setContent("Once upon a time.");
        story.setLanguage("uk");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        lenient().when(storyRepository.findById("story-1")).thenReturn(Optional.of(story));
    }

    @Test
    void should_renderSinglePageAndMarkReady_when_storyPending() {
        when(structurer.structure(anyString(), eq("uk"))).thenReturn(Mono.just(fiveBeats()));
        byte[] png = new byte[]{1, 2, 3};
        when(nanoBanana.generate(anyString(), eq(PanelAspect.PAGE), isNull())).thenReturn(Mono.just(png));
        when(imageStorage.storePanel(eq("story-1"), eq(1), any(byte[].class))).thenReturn("path/key");
        when(panelRepository.save(any(StoryPanel.class))).thenAnswer(inv -> inv.getArgument(0));

        builder.build("story-1").block();

        verify(nanoBanana, times(1)).generate(anyString(), eq(PanelAspect.PAGE), isNull());

        ArgumentCaptor<StoryPanel> captor = ArgumentCaptor.forClass(StoryPanel.class);
        verify(panelRepository, times(1)).save(captor.capture());
        StoryPanel saved = captor.getValue();
        assertThat(saved.getPanelIndex()).isEqualTo(1);
        assertThat(saved.getAspect()).isEqualTo(PanelAspect.PAGE);
        assertThat(saved.getImagePath()).isEqualTo("path/key");

        assertThat(story.getIllustrationStatus()).isEqualTo(IllustrationStatus.READY);
        verify(storyRepository).save(story);
        verify(pushNotifier).notifyStoryReady("user-1", "story-1", "Test");
    }

    @Test
    void should_markFailed_when_structurerFails() {
        when(structurer.structure(anyString(), anyString())).thenReturn(Mono.error(new RuntimeException("boom")));

        builder.build("story-1").block();

        assertThat(story.getIllustrationStatus()).isEqualTo(IllustrationStatus.FAILED);
        verifyNoInteractions(nanoBanana);
        verify(storyRepository).save(story);
        verify(pushNotifier, never()).notifyStoryReady(anyString(), anyString(), anyString());
    }

    @Test
    void should_markFailed_when_imageGenerationFails() {
        when(structurer.structure(anyString(), anyString())).thenReturn(Mono.just(fiveBeats()));
        when(nanoBanana.generate(anyString(), eq(PanelAspect.PAGE), isNull()))
                .thenReturn(Mono.error(new RuntimeException("nb-503")));

        builder.build("story-1").block();

        assertThat(story.getIllustrationStatus()).isEqualTo(IllustrationStatus.FAILED);
        verify(panelRepository, never()).save(any(StoryPanel.class));
    }

    @Test
    void should_skip_when_storyNotPending() {
        story.setIllustrationStatus(IllustrationStatus.READY);

        builder.build("story-1").block();

        verifyNoInteractions(structurer, nanoBanana, panelRepository);
    }

    @Test
    void should_skip_when_storyNotFound() {
        // storyRepository.findById returns Optional.empty() by default for an unknown id.
        builder.build("missing").block();

        verifyNoInteractions(structurer, nanoBanana, panelRepository);
    }

    @Test
    void should_treatDuplicateAsAlreadyBuilt_when_concurrentInsertHitsUniqueKey() {
        when(structurer.structure(anyString(), anyString())).thenReturn(Mono.just(fiveBeats()));
        when(nanoBanana.generate(anyString(), eq(PanelAspect.PAGE), isNull())).thenReturn(Mono.just(new byte[]{9}));
        when(imageStorage.storePanel(eq("story-1"), eq(1), any(byte[].class))).thenReturn("dupe/key");
        when(panelRepository.save(any(StoryPanel.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup uk_story_panels"));

        builder.build("story-1").block();

        // A concurrent build already produced the comic — must NOT clobber it to FAILED.
        assertThat(story.getIllustrationStatus()).isEqualTo(IllustrationStatus.READY);
        // Must NOT delete the blob: both racing builds use the same deterministic key
        // (storyId-p1.png), so the file on disk is also what the winning row references.
        // Deleting it leaves the surviving DB row pointing at nothing.
        verify(imageStorage, never()).deleteByKey(anyString());
    }
}
