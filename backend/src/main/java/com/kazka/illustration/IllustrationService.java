package com.kazka.illustration;

import com.kazka.config.OllamaProperties;
import com.kazka.ollama.OllamaClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class IllustrationService {

    private static final Logger log = LoggerFactory.getLogger(IllustrationService.class);

    private final OllamaClient ollamaClient;
    private final OllamaProperties ollamaProperties;
    private final ImageStorageService imageStorageService;
    private final StoryRepository storyRepository;
    private final PromptBuilder promptBuilder;

    public IllustrationService(OllamaClient ollamaClient, OllamaProperties ollamaProperties,
                               ImageStorageService imageStorageService, StoryRepository storyRepository,
                               PromptBuilder promptBuilder) {
        this.ollamaClient = ollamaClient;
        this.ollamaProperties = ollamaProperties;
        this.imageStorageService = imageStorageService;
        this.storyRepository = storyRepository;
        this.promptBuilder = promptBuilder;
    }

    public Mono<Void> generateAndStore(String storyId) {
        return Mono.fromCallable(() -> storyRepository.findById(storyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()))
                .flatMap(story -> {
                    String prompt = promptBuilder.buildIllustrationPrompt(
                            story.getTitle(), story.getCharacters());
                    return ollamaClient.generateImage(ollamaProperties.getImageModel(), prompt)
                            .flatMap(base64 -> saveImage(story, base64))
                            .switchIfEmpty(markFailed(story))
                            .onErrorResume(e -> {
                                log.warn("Illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    private Mono<Void> saveImage(Story story, String base64) {
        return Mono.fromRunnable(() -> {
            String path = imageStorageService.save(story.getId(), base64);
            story.setIllustrationPath(path);
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
}
