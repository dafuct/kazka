package com.kazka.child.bedtime;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.child.bedtime.dto.BedtimeScheduleDto;
import com.kazka.child.bedtime.dto.BedtimeUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/children/{childId}/bedtime")
public class BedtimeScheduleController {

    private final BedtimeScheduleService svc;
    private final CurrentUserResolver currentUserResolver;

    public BedtimeScheduleController(BedtimeScheduleService svc, CurrentUserResolver currentUserResolver) {
        this.svc = svc;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public Mono<BedtimeScheduleDto> get(@PathVariable String childId) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() ->
                        BedtimeScheduleDto.from(svc.getOrEmpty(childId, cu.userId())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping
    public Mono<BedtimeScheduleDto> upsert(@PathVariable String childId,
                                           @Valid @RequestBody BedtimeUpdateRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() ->
                        BedtimeScheduleDto.from(svc.upsert(childId, cu.userId(), req)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
