package com.kazka.story;

import com.kazka.config.OllamaProperties;
import com.kazka.illustration.IllustrationService;
import com.kazka.ollama.OllamaClient;
import com.kazka.story.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
public class StoryService {

    private final StoryRepository repository;
    private final OllamaClient ollamaClient;
    private final OllamaProperties ollamaProperties;
    private final PromptBuilder promptBuilder;
    private final IllustrationService illustrationService;

    public StoryService(StoryRepository repository, OllamaClient ollamaClient,
                        OllamaProperties ollamaProperties, PromptBuilder promptBuilder,
                        IllustrationService illustrationService) {
        this.repository = repository;
        this.ollamaClient = ollamaClient;
        this.ollamaProperties = ollamaProperties;
        this.promptBuilder = promptBuilder;
        this.illustrationService = illustrationService;
    }

    public Flux<SseEvent> generate(GenerationRequest req) {
        String id = UUID.randomUUID().toString();
        String prompt = promptBuilder.buildPrompt(req);

        Story story = new Story();
        story.setId(id);
        story.setTitle("");
        story.setTheme(req.theme());
        story.setCharacters(req.characters());
        story.setAgeGroup(req.ageGroup());
        story.setLength(req.length());
        story.setLanguage(req.language());
        story.setContent("");
        story.setIllustrationStatus(IllustrationStatus.PENDING);

        return Mono.fromCallable(() -> repository.save(story))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(saved -> {
                    Flux<SseEvent> meta = Flux.just(SseEvent.meta(id));

                    StringBuilder contentBuffer = new StringBuilder();
                    Flux<SseEvent> tokens = ollamaClient
                            .streamGenerate(ollamaProperties.getTextModel(), prompt)
                            .doOnNext(contentBuffer::append)
                            .map(SseEvent::token)
                            .concatWith(Mono.fromCallable(() -> {
                                String fullContent = contentBuffer.toString();
                                String[] lines = fullContent.split("\n", 2);
                                String title = lines[0].strip();
                                saved.setTitle(title);
                                saved.setContent(fullContent);
                                repository.save(saved);
                                return SseEvent.done(id, title);
                            }).subscribeOn(Schedulers.boundedElastic()))
                            .onErrorResume(e -> Flux.just(SseEvent.error(e.getMessage())));

                    return meta.concatWith(tokens);
                });
    }

    public Mono<Void> illustrate(String id) {
        return illustrationService.generateAndStore(id)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<PageResponse<StoryDto>> list(int page, int size) {
        return Mono.fromCallable(() -> {
            Page<Story> p = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
            return new PageResponse<>(
                    p.getContent().stream().map(StoryDto::from).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> findById(String id) {
        return Mono.fromCallable(() -> repository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(opt -> opt.map(StoryDto::from)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public Mono<StoryDto> update(String id, UpdateStoryRequest req) {
        return Mono.fromCallable(() -> repository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> {
                    Story story = opt.orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    story.setTitle(req.title());
                    story.setContent(req.content());
                    return Mono.fromCallable(() -> repository.save(story))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .map(StoryDto::from);
    }

    public Mono<Void> delete(String id) {
        return Mono.fromCallable(() -> repository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> {
                    Story story = opt.orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    return Mono.fromRunnable(() -> {
                        if (story.getIllustrationPath() != null) {
                            illustrationService.deleteImage(id);
                        }
                        repository.deleteById(id);
                    }).subscribeOn(Schedulers.boundedElastic());
                }).then();
    }
}
