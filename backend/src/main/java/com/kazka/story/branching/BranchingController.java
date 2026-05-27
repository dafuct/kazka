package com.kazka.story.branching;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.story.branching.dto.BranchingChoiceRequest;
import com.kazka.story.branching.dto.BranchingResponse;
import com.kazka.story.branching.dto.BranchingStartRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class BranchingController {

    private final BranchingService svc;
    private final CurrentUserResolver currentUserResolver;

    public BranchingController(BranchingService svc, CurrentUserResolver currentUserResolver) {
        this.svc = svc;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/stories/branching")
    public Mono<BranchingResponse> start(@Valid @RequestBody BranchingStartRequest req) {
        return currentUserResolver.requireUser().flatMap(cu -> svc.start(req, cu));
    }

    @PostMapping("/api/stories/{id}/branching/choose")
    public Mono<BranchingResponse> choose(@PathVariable String id,
                                          @Valid @RequestBody BranchingChoiceRequest req) {
        return currentUserResolver.requireUser().flatMap(cu -> svc.choose(id, req.choiceId(), cu));
    }
}
