package com.kazka.moderation;

import com.kazka.story.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/admin/moderation")
public class AdminModerationController {

    private final AdminModerationService service;

    public AdminModerationController(AdminModerationService service) {
        this.service = service;
    }

    @GetMapping("/flagged")
    public Mono<PageResponse<FlaggedAttemptDto>> listFlagged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Mono.fromCallable(() -> service.listFlagged(page, size))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/suspended")
    public Mono<List<SuspendedUserDto>> listSuspended() {
        return Mono.fromCallable(service::listSuspended)
                .subscribeOn(Schedulers.boundedElastic());
    }
}
