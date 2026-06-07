package com.kazka.child;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.child.dto.ExtractedCandidateDto;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class ExtractionController {

    private final CharacterExtractionService extraction;
    private final StoryRepository stories;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/api/stories/{id}/extraction-candidates")
    public Mono<List<ExtractedCandidateDto>> candidates(@PathVariable String id,
                                                        @RequestParam(required = false) String lang) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> {
                    Story story = stories.findByIdAndUserId(id, cu.userId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    return story;
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(story -> extraction.extract(story.getContent(),
                        lang != null && !lang.isBlank() ? lang : story.getLanguage()));
    }
}
