package com.kazka.story.showcase;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/showcase")
public class ShowcaseController {

    private final ShowcaseService service;

    @GetMapping
    public Mono<List<ShowcaseStoryDto>> list() {
        return Mono.fromCallable(service::list).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{id}")
    public Mono<ShowcaseStoryDto> get(@PathVariable String id) {
        return Mono.fromCallable(() -> service.get(id)).subscribeOn(Schedulers.boundedElastic());
    }
}
