package com.kazka.story;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.illustration.IllustrationService;
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
    private final HuggingFaceClient hfClient;
    private final PromptBuilder promptBuilder;
    private final IllustrationService illustrationService;

    public StoryService(StoryRepository repository, HuggingFaceClient hfClient,
                        PromptBuilder promptBuilder, IllustrationService illustrationService) {
        this.repository = repository;
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.illustrationService = illustrationService;
    }

    public Flux<SseEvent> generate(GenerationRequest req) {
        String id = UUID.randomUUID().toString();
        String storySystem = promptBuilder.buildStorySystem();
        String storyUser = promptBuilder.buildStoryUserMessage(req);
        String editorSystem = promptBuilder.buildEditorSystem(req.language());

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

                    StringBuilder rawBuffer = new StringBuilder();
                    // Step 1: stream raw story to user; Step 2: edit silently, save corrected version
                    Flux<SseEvent> tokens = hfClient.streamText(storySystem, storyUser)
                            .doOnNext(rawBuffer::append)
                            .map(SseEvent::token)
                            .concatWith(Mono.defer(() ->
                                hfClient.streamEdit(editorSystem, rawBuffer.toString())
                                        .reduce("", String::concat)
                                        .flatMap(corrected -> Mono.fromCallable(() -> {
                                            String[] lines = corrected.split("\n");
                                            String title = "";
                                            int storyStart = 0;
                                            for (int i = 0; i < lines.length; i++) {
                                                String l = lines[i].strip();
                                                if (!l.isEmpty()) { title = l; storyStart = i + 1; break; }
                                            }
                                            // skip blank lines after title to get clean story body
                                            while (storyStart < lines.length && lines[storyStart].strip().isEmpty()) storyStart++;
                                            String body = String.join("\n", java.util.Arrays.copyOfRange(lines, storyStart, lines.length));
                                            saved.setTitle(title);
                                            saved.setContent(body);
                                            repository.save(saved);
                                            return SseEvent.done(id, title);
                                        }).subscribeOn(Schedulers.boundedElastic()))
                            ))
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
