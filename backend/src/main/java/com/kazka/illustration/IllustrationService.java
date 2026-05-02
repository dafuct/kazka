package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class IllustrationService {

    private static final Logger log = LoggerFactory.getLogger(IllustrationService.class);

    private final HuggingFaceClient hfClient;
    private final ImageStorageService imageStorageService;
    private final StoryRepository storyRepository;

    public IllustrationService(HuggingFaceClient hfClient,
                               ImageStorageService imageStorageService,
                               StoryRepository storyRepository) {
        this.hfClient = hfClient;
        this.imageStorageService = imageStorageService;
        this.storyRepository = storyRepository;
    }

    public Mono<Void> generateAndStore(String storyId) {
        return Mono.fromCallable(() -> storyRepository.findById(storyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()))
                .flatMap(story -> {
                    String chars = story.getCharacters() == null ? "" : String.join(", ", story.getCharacters());
                    String prompt = story.getTitle() + " featuring " + chars;
                    return hfClient.generateImage(prompt)
                            .flatMap(bytes -> saveImage(story, bytes).thenReturn(Boolean.TRUE))
                            .defaultIfEmpty(Boolean.FALSE)
                            .flatMap(ok -> {
                                if (!ok) return markFailed(story);
                                return Mono.empty();
                            })
                            .onErrorResume(e -> {
                                log.warn("Illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    private Mono<Void> saveImage(Story story, byte[] imageBytes) {
        return Mono.fromRunnable(() -> {
            String path = imageStorageService.save(story.getId(), imageBytes);
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

    public void deleteImage(String storyId) {
        imageStorageService.delete(storyId);
    }
}
