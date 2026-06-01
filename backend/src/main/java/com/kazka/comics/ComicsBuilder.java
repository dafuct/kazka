package com.kazka.comics;

import com.kazka.config.AiProviderProperties;
import com.kazka.device.PushNotifier;
import com.kazka.illustration.ImageStorage;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Builds a single comic PAGE for a finished tale:
 *  1) one Gemini structuring call → {@value ActsStructurer#BEATS} beats
 *  2) compose one page prompt ({@link ComicPagePrompt})
 *  3) one Nano Banana call → one PNG of the whole page
 *  4) persist a single {@code story_panels} row + push notification
 *
 * Bounded by kazka.ai.comics.pipeline-timeout (default 60s). {@link #build} is a
 * no-op unless the story is PENDING, so a stray second trigger can't duplicate the
 * panel row or clobber a READY story.
 */
@Slf4j
@Service
public class ComicsBuilder {

    private final ActsStructurer structurer;
    private final NanoBananaClient nanoBanana;
    private final ImageStorage imageStorage;
    private final StoryRepository storyRepository;
    private final StoryPanelRepository panelRepository;
    private final PushNotifier pushNotifier;
    private final Duration pipelineTimeout;

    @Autowired
    public ComicsBuilder(ActsStructurer structurer,
                         NanoBananaClient nanoBanana,
                         ImageStorage imageStorage,
                         StoryRepository storyRepository,
                         StoryPanelRepository panelRepository,
                         PushNotifier pushNotifier,
                         AiProviderProperties aiProps) {
        this(structurer, nanoBanana, imageStorage, storyRepository, panelRepository,
             pushNotifier, aiProps.getComics().getPipelineTimeout());
    }

    // Test-friendly constructor — avoids needing AiProviderProperties in unit tests.
    ComicsBuilder(ActsStructurer structurer,
                  NanoBananaClient nanoBanana,
                  ImageStorage imageStorage,
                  StoryRepository storyRepository,
                  StoryPanelRepository panelRepository,
                  PushNotifier pushNotifier,
                  Duration pipelineTimeout) {
        this.structurer = structurer;
        this.nanoBanana = nanoBanana;
        this.imageStorage = imageStorage;
        this.storyRepository = storyRepository;
        this.panelRepository = panelRepository;
        this.pushNotifier = pushNotifier;
        this.pipelineTimeout = pipelineTimeout;
    }

    public Mono<Void> build(String storyId) {
        return Mono.fromCallable(() -> storyRepository.findById(storyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> opt
                        // Only build a story that is still PENDING and has no page yet — guards
                        // against a stray second trigger (e.g. a manual POST /illustrate racing
                        // the server-side trigger) re-running the pipeline.
                        .filter(s -> s.getIllustrationStatus() == IllustrationStatus.PENDING
                                && panelRepository.countByStoryId(s.getId()) == 0)
                        .map(Mono::just).orElse(Mono.empty()))
                .flatMap(this::pipeline)
                .timeout(pipelineTimeout)
                .onErrorResume(e -> {
                    log.warn("Comics pipeline failed for story={}: {}", storyId, e.toString());
                    return Mono.fromRunnable(() -> markFailed(storyId))
                            .subscribeOn(Schedulers.boundedElastic()).then();
                });
    }

    private Mono<Void> pipeline(Story story) {
        return structurer.structure(story.getContent(), story.getLanguage())
                .map(beats -> ComicPagePrompt.compose(beats, story.getLanguage()))
                .flatMap(prompt -> nanoBanana.generate(prompt, PanelAspect.PAGE, null)
                        .flatMap(bytes -> persistPage(story.getId(), prompt, bytes)))
                .then(Mono.fromRunnable(() -> markReady(story)).subscribeOn(Schedulers.boundedElastic()))
                .then(Mono.fromRunnable(() -> firePush(story)).subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    private Mono<Void> persistPage(String storyId, String prompt, byte[] bytes) {
        return Mono.fromRunnable(() -> {
            String key = imageStorage.storePanel(storyId, 1, bytes);
            StoryPanel panel = new StoryPanel();
            panel.setId(UUID.randomUUID().toString());
            panel.setStoryId(storyId);
            panel.setPanelIndex(1);
            panel.setImagePath(key);
            panel.setScenePrompt(prompt);
            panel.setNarration("");
            panel.setAspect(PanelAspect.PAGE);
            try {
                panelRepository.save(panel);
            } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                // A concurrent build already inserted this story's page — the unique key on
                // (story_id, panel_index) rejects the duplicate. Treat as success: letting it
                // propagate would flip the story to FAILED via onErrorResume and clobber the
                // good comic the other build produced. Do NOT delete the file: both builds use
                // the same deterministic key, so the on-disk PNG is also what the winning row
                // references — deleting it would leave the winner's row pointing at nothing.
                log.info("Comic page already persisted for story={} (concurrent build won the race); discarding duplicate", storyId);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private void markReady(Story story) {
        story.setIllustrationStatus(IllustrationStatus.READY);
        storyRepository.save(story);
    }

    private void markFailed(String storyId) {
        storyRepository.findById(storyId).ifPresent(s -> {
            s.setIllustrationStatus(IllustrationStatus.FAILED);
            storyRepository.save(s);
        });
    }

    private void firePush(Story story) {
        try {
            pushNotifier.notifyStoryReady(story.getUserId(), story.getId(),
                    story.getTitle() == null ? "" : story.getTitle());
        } catch (Exception e) {
            log.warn("Push hook failed after comics for story={}: {}", story.getId(), e.getMessage());
        }
    }

    /** Delete all panels (DB rows + storage objects) for a story. Used by delete and retry flows.
     *  Transactional because {@code deleteByStoryId} is a JPA bulk delete and would otherwise throw
     *  {@code TransactionRequiredException}. */
    @Transactional
    public void deletePanels(String storyId) {
        List<StoryPanel> existing = panelRepository.findByStoryIdOrderByPanelIndexAsc(storyId);
        existing.forEach(p -> {
            try { imageStorage.deleteByKey(p.getImagePath()); }
            catch (Exception e) { log.warn("Failed to delete panel image {}: {}", p.getImagePath(), e.getMessage()); }
        });
        panelRepository.deleteByStoryId(storyId);
    }

    /** For retry endpoint: wipes existing panels, sets status PENDING, kicks off a new build. */
    public Mono<Void> retry(String storyId) {
        return Mono.fromRunnable(() -> {
            deletePanels(storyId);
            storyRepository.findById(storyId).ifPresent(s -> {
                s.setIllustrationStatus(IllustrationStatus.PENDING);
                storyRepository.save(s);
            });
        }).subscribeOn(Schedulers.boundedElastic()).then(build(storyId));
    }
}
