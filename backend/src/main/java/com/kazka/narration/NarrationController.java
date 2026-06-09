package com.kazka.narration;

import com.kazka.auth.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Read-aloud narration endpoints. POST triggers (or returns) the cached narration; GET polls status.
 * Lives under /api/stories/** so it inherits the same auth + CSRF double-submit protection.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/stories")
public class NarrationController {

    private final NarrationService narrationService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/{id}/narration")
    public Mono<ResponseEntity<NarrationResponse>> requestNarration(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> narrationService.requestNarration(id, cu))
                .map(resp -> "READY".equals(resp.status())
                        ? ResponseEntity.ok(resp)
                        : ResponseEntity.accepted().body(resp));
    }

    @GetMapping("/{id}/narration")
    public Mono<NarrationResponse> getNarration(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> narrationService.getNarration(id, cu));
    }
}
