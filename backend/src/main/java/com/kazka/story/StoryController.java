package com.kazka.story;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.story.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;
    private final CurrentUserResolver currentUserResolver;

    public StoryController(StoryService storyService, CurrentUserResolver currentUserResolver) {
        this.storyService = storyService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> generate(@Valid @RequestBody GenerationRequest req) {
        return currentUserResolver.requireUser()
                .flatMapMany(cu -> storyService.generate(req, cu))
                .map(event -> ServerSentEvent.builder()
                        .event(event.type())
                        .data(event.data())
                        .build());
    }

    @PostMapping("/{id}/illustrate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> illustrate(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> {
                    storyService.illustrate(id, cu).subscribe();
                    return Mono.empty();
                });
    }

    @GetMapping
    public Mono<PageResponse<StoryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.list(page, size, cu));
    }

    @GetMapping("/cursor")
    public Mono<CursorPageResponse<StoryDto>> listByCursor(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        int effective = Math.min(Math.max(limit, 1), 100);
        return currentUserResolver.requireUser()
                .flatMap(cu -> storyService.listByCursor(
                        cursor == null || cursor.isBlank() ? null : cursor,
                        effective,
                        cu));
    }

    @GetMapping("/featured")
    public Mono<ResponseEntity<StoryDto>> featured() {
        return currentUserResolver.requireUser()
                .flatMap(cu -> storyService.featured(cu))
                .map(dto -> ResponseEntity.ok(dto))
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public Mono<StoryDto> findById(@PathVariable String id) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.findById(id, cu));
    }

    @PutMapping("/{id}")
    public Mono<StoryDto> update(@PathVariable String id, @Valid @RequestBody UpdateStoryRequest req) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.update(id, req, cu));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.delete(id, cu));
    }

    @PostMapping("/{id}/extract-characters")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> reextract(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> storyService.triggerExtraction(id, cu));
    }
}
