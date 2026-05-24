package com.kazka.child;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.child.dto.ChildProfileDto;
import com.kazka.child.dto.CreateChildProfileRequest;
import com.kazka.child.dto.UpdateChildProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/children")
public class ChildProfileController {

    private final ChildProfileService svc;
    private final ChildRateLimiter rateLimiter;
    private final CurrentUserResolver currentUserResolver;

    public ChildProfileController(ChildProfileService svc,
                                  ChildRateLimiter rateLimiter,
                                  CurrentUserResolver currentUserResolver) {
        this.svc = svc;
        this.rateLimiter = rateLimiter;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    public Mono<ChildProfileDto> create(@Valid @RequestBody CreateChildProfileRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> {
                    rateLimiter.assertAndIncrement(cu.userId());
                    ChildProfile p = svc.create(cu.userId(), req);
                    return ChildProfileDto.from(p, svc.countCharacters(p.getId()));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<List<ChildProfileDto>> list() {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> svc.listActive(cu.userId()).stream()
                        .map(p -> ChildProfileDto.from(p, svc.countCharacters(p.getId())))
                        .toList()
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}")
    public Mono<ChildProfileDto> findById(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> {
                    ChildProfile p = svc.requireOwned(id, cu.userId());
                    return ChildProfileDto.from(p, svc.countCharacters(p.getId()));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @PatchMapping("/{id}")
    public Mono<ChildProfileDto> update(@PathVariable String id, @Valid @RequestBody UpdateChildProfileRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> {
                    ChildProfile p = svc.update(id, cu.userId(), req);
                    return ChildProfileDto.from(p, svc.countCharacters(p.getId()));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> archive(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromRunnable(() -> svc.archive(id, cu.userId()))
                        .subscribeOn(Schedulers.boundedElastic()).then());
    }
}
