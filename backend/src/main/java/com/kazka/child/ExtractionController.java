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
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class ExtractionController {

    private final CharacterExtractionService extraction;
    private final StoryRepository stories;
    private final StoryCharacterRepository storyCharacters;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/api/stories/{id}/extraction-candidates")
    public Mono<List<ExtractedCandidateDto>> candidates(@PathVariable String id,
                                                        @RequestParam(required = false) String lang) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> {
                    Story story = stories.findByIdAndUserId(id, cu.userId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    // Once the user confirms characters for this tale, StoryCharacter join rows
                    // exist (see CharacterService.upsertConfirmed) — a persistent "already saved"
                    // signal that survives a page reload. Suppress the panel from then on instead
                    // of re-deriving (and re-offering) the same candidates via a fresh LLM call.
                    boolean alreadyConfirmed = !storyCharacters.findById_StoryId(id).isEmpty();
                    return alreadyConfirmed ? Optional.<Story>empty() : Optional.of(story);
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(maybeStory -> maybeStory
                        .map(story -> extraction.extract(story.getContent(),
                                lang != null && !lang.isBlank() ? lang : story.getLanguage()))
                        .orElseGet(() -> Mono.just(List.<ExtractedCandidateDto>of())));
    }
}
