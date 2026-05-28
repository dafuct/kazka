package com.kazka.child;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.child.dto.CharacterDto;
import com.kazka.child.dto.ConfirmCharactersRequest;
import com.kazka.child.dto.UpdateCharacterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class CharacterController {

    private final CharacterService svc;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/api/children/{childId}/characters")
    public Mono<List<CharacterDto>> list(@PathVariable String childId) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> svc.listForProfile(childId, cu.userId()).stream()
                        .map(CharacterDto::from).toList())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/api/characters/confirm")
    public Mono<List<CharacterDto>> confirm(@RequestParam String childProfileId,
                                            @Valid @RequestBody ConfirmCharactersRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> svc
                        .upsertConfirmed(childProfileId, cu.userId(), req.storyId(), req.candidates())
                        .stream().map(CharacterDto::from).toList())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PatchMapping("/api/characters/{id}")
    public Mono<CharacterDto> update(@PathVariable String id, @Valid @RequestBody UpdateCharacterRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() ->
                        CharacterDto.from(svc.updateOwned(id, cu.userId(),
                                req.name(), req.kind(), req.description(), req.traits())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/api/characters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> archive(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromRunnable(() -> svc.archive(id, cu.userId()))
                        .subscribeOn(Schedulers.boundedElastic()).then());
    }
}
