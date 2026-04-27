package com.kazka.story;

import com.kazka.story.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> generate(@Valid @RequestBody GenerationRequest req) {
        return storyService.generate(req)
                .map(event -> ServerSentEvent.builder()
                        .event(event.type())
                        .data(event.data())
                        .build());
    }

    @PostMapping("/{id}/illustrate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> illustrate(@PathVariable String id) {
        storyService.illustrate(id).subscribe();
        return Mono.empty();
    }

    @GetMapping
    public Mono<PageResponse<StoryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return storyService.list(page, size);
    }

    @GetMapping("/{id}")
    public Mono<StoryDto> findById(@PathVariable String id) {
        return storyService.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<StoryDto> update(@PathVariable String id,
                                  @Valid @RequestBody UpdateStoryRequest req) {
        return storyService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return storyService.delete(id);
    }
}
