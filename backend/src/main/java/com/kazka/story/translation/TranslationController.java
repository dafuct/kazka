package com.kazka.story.translation;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.story.dto.StoryDto;
import com.kazka.story.translation.dto.TranslateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class TranslationController {

    private final TranslationService svc;
    private final CurrentUserResolver currentUserResolver;

    public TranslationController(TranslationService svc, CurrentUserResolver currentUserResolver) {
        this.svc = svc;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/stories/{id}/translate")
    public Mono<StoryDto> translate(@PathVariable String id,
                                    @Valid @RequestBody TranslateRequest req) {
        return currentUserResolver.requireUser().flatMap(cu -> svc.translate(id, req.targetLanguage(), cu));
    }
}
