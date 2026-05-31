package com.kazka.illustration;

import com.kazka.ai.AiClient;
import com.kazka.device.PushNotifier;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class IllustrationService {

    private static final int IMAGE_W = 1024;
    private static final int IMAGE_H = 768;

    private final AiClient aiClient;
    private final ImageStorage imageStorage;
    private final StoryRepository storyRepository;
    private final PromptBuilder promptBuilder;
    private final ModerationService moderationService;
    private final SuspensionService suspensionService;
    private final ModerationProperties modProps;
    private final PushNotifier pushNotifier;

    public Mono<Void> generateAndStore(String storyId) {
        return Mono.fromCallable(() -> storyRepository.findById(storyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()))
                .flatMap(story -> {
                    List<String> chars = story.getCharacters();
                    String firstChar = (chars != null && !chars.isEmpty()) ? chars.get(0) : "a character";
                    String fallbackOnError = firstChar + " in a magical scene from " + story.getTitle();

                    return aiClient.generateText(
                                    promptBuilder.buildSceneExtractionSystem(),
                                    promptBuilder.buildSceneExtractionUser(story.getContent()))
                            .onErrorReturn(fallbackOnError)
                            .map(scene -> scene.isBlank() ? fallbackOnError : scene)
                            // chooseSafeScene calls ModerationJudgeClient which uses .block() on
                            // the WebClient response. Reactor refuses block() on reactor-http
                            // threads, so we hop to boundedElastic for this step. The judge call
                            // is short (sub-second judge timeout) and the surrounding LLM/image
                            // calls already dominate the wall-clock cost.
                            .flatMap(scene -> Mono.fromCallable(() -> chooseSafeScene(story, scene))
                                    .subscribeOn(Schedulers.boundedElastic()))
                            .flatMap(scene -> Mono.zip(
                                    aiClient.generateImage(promptBuilder.buildImagePrompt(story, scene, Theme.LIGHT), IMAGE_W, IMAGE_H),
                                    aiClient.generateImage(promptBuilder.buildImagePrompt(story, scene, Theme.DARK), IMAGE_W, IMAGE_H)))
                            .flatMap(tuple -> savePair(story, tuple.getT1(), tuple.getT2())
                                    .doOnSuccess(v -> {
                                        try {
                                            pushNotifier.notifyStoryReady(story.getUserId(), story.getId(),
                                                    story.getTitle() == null ? "" : story.getTitle());
                                        } catch (Exception e) {
                                            log.warn("Push hook failed after illustration for story={}: {}", story.getId(), e.getMessage());
                                        }
                                    }))
                            .onErrorResume(e -> {
                                log.warn("PNG illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    /**
     * Run scene moderation; on a non-JUDGE_UNAVAILABLE refusal, log an IMAGE_SCENE flag
     * (which SuspensionService excludes from the suspension count) and swap to the
     * configured safe fallback scene. JUDGE_UNAVAILABLE falls through to the original
     * scene — image generation is best-effort and not worth blocking on a transient
     * judge outage.
     */
    private String chooseSafeScene(Story story, String scene) {
        ModerationResult r = moderationService.checkScene(story.getLanguage(), scene);
        if (r instanceof ModerationResult.Refused refused
                && refused.category() != ModerationCategory.JUDGE_UNAVAILABLE) {
            try {
                suspensionService.recordAndMaybeSuspend(
                        story.getUserId(),
                        ModerationPipeline.IMAGE_SCENE,
                        refused.category(),
                        story.getLanguage(),
                        scene,
                        refused.confidence(),
                        modProps.getJudgeModel());
            } catch (Exception logFailure) {
                log.warn("Failed to log image-scene flag: {}", logFailure.getMessage());
            }
            return modProps.getSafeFallbackScene();
        }
        return scene;
    }

    private Mono<Void> savePair(Story story, byte[] light, byte[] dark) {
        return Mono.fromRunnable(() -> {
            String lightKey = imageStorage.store(story.getId(), Theme.LIGHT, light);
            String darkKey = imageStorage.store(story.getId(), Theme.DARK, dark);
            story.setIllustrationPathLight(lightKey);
            story.setIllustrationPathDark(darkKey);
            story.setIllustrationStatus(IllustrationStatus.READY);
            storyRepository.save(story);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<Void> markFailed(Story story) {
        return Mono.fromRunnable(() -> {
            story.setIllustrationStatus(IllustrationStatus.FAILED);
            storyRepository.save(story);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public void deleteImage(String storyId) {
        imageStorage.delete(storyId);
    }
}
