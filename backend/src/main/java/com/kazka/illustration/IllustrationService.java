package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class IllustrationService {

    private static final Logger log = LoggerFactory.getLogger(IllustrationService.class);
    private static final int IMAGE_W = 1024;
    private static final int IMAGE_H = 768;

    private final HuggingFaceClient hfClient;
    private final ImageStorageService imageStorageService;
    private final StoryRepository storyRepository;
    private final PromptBuilder promptBuilder;

    public IllustrationService(HuggingFaceClient hfClient,
                               ImageStorageService imageStorageService,
                               StoryRepository storyRepository,
                               PromptBuilder promptBuilder) {
        this.hfClient = hfClient;
        this.imageStorageService = imageStorageService;
        this.storyRepository = storyRepository;
        this.promptBuilder = promptBuilder;
    }

    public Mono<Void> generateAndStore(String storyId) {
        return Mono.fromCallable(() -> storyRepository.findById(storyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()))
                .flatMap(story -> {
                    List<String> chars = story.getCharacters();
                    String firstChar = (chars != null && !chars.isEmpty()) ? chars.get(0) : "a character";
                    String fallback = firstChar + " in a magical scene from " + story.getTitle();

                    return hfClient.generateText(
                                    promptBuilder.buildSceneExtractionSystem(),
                                    promptBuilder.buildSceneExtractionUser(story.getContent()))
                            .onErrorReturn(fallback)
                            .map(scene -> scene.isBlank() ? fallback : scene)
                            .flatMap(scene -> Mono.zip(
                                    hfClient.generateImage(
                                            promptBuilder.buildImagePrompt(story, scene, Theme.LIGHT),
                                            IMAGE_W, IMAGE_H),
                                    hfClient.generateImage(
                                            promptBuilder.buildImagePrompt(story, scene, Theme.DARK),
                                            IMAGE_W, IMAGE_H)
                            ))
                            .flatMap(tuple -> savePair(story, tuple.getT1(), tuple.getT2()))
                            .onErrorResume(e -> {
                                log.warn("PNG illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    private Mono<Void> savePair(Story story, byte[] light, byte[] dark) {
        return Mono.fromRunnable(() -> {
            String lightPath = imageStorageService.savePng(story.getId(), Theme.LIGHT, light);
            String darkPath = imageStorageService.savePng(story.getId(), Theme.DARK, dark);
            story.setIllustrationPathLight(lightPath);
            story.setIllustrationPathDark(darkPath);
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
        imageStorageService.delete(storyId);
    }
}
