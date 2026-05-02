package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class IllustrationService {

    private static final Logger log = LoggerFactory.getLogger(IllustrationService.class);

    private static final String PLACEHOLDER_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 800 600\" width=\"800\" height=\"600\">" +
            "<rect width=\"800\" height=\"600\" fill=\"#fde8c8\"/>" +
            "</svg>";

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
                            .flatMap(scene ->
                                    hfClient.generateText(
                                            promptBuilder.buildSvgSystem(),
                                            promptBuilder.buildSvgUser(story, scene)))
                            .onErrorReturn(PLACEHOLDER_SVG)
                            .map(this::extractSvgTag)
                            .flatMap(svg -> saveSvg(story, svg))
                            .onErrorResume(e -> {
                                log.warn("SVG illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    private String extractSvgTag(String raw) {
        int start = raw.indexOf("<svg");
        int end = raw.lastIndexOf("</svg>") + 6;
        if (start == -1 || end < 6) {
            log.warn("LLM response contained no <svg> tag; using placeholder");
            return PLACEHOLDER_SVG;
        }
        return raw.substring(start, end);
    }

    private Mono<Void> saveSvg(Story story, String svgText) {
        return Mono.fromRunnable(() -> {
            String path = imageStorageService.saveSvg(story.getId(), svgText);
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
