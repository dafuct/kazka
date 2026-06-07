package com.kazka.story.showcase;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/showcase")
public class ShowcaseImageController {

    private final ShowcaseService service;

    @GetMapping(value = "/{storyId}/image/{key}", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<Resource> image(@PathVariable String storyId, @PathVariable String key) {
        return Mono.fromCallable(() ->
                        (Resource) new FileSystemResource(service.resolveShowcaseImage(storyId, key)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
