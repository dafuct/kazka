package com.kazka.narration;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.story.NarrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Orchestrates lazy, cached read-aloud narration. First request for a tale atomically claims it,
 * synthesizes via the configured {@link TtsClient} off the request thread (fire-and-forget),
 * stores the audio, and caches its key; later requests return the cached presigned URL. Admins
 * may narrate any tale (admin-aware lookup). The active client owns voice selection (per tale
 * language) and audio container, so this service stays provider-agnostic.
 */
@Slf4j
@Service
public class NarrationService {

    private final StoryRepository repository;
    private final TtsClient ttsClient;
    private final AudioStorage audioStorage;

    public NarrationService(StoryRepository repository, TtsClient ttsClient, AudioStorage audioStorage) {
        this.repository = repository;
        this.ttsClient = ttsClient;
        this.audioStorage = audioStorage;
    }

    /** POST: return cached URL if READY, else claim + trigger async synthesis and report GENERATING. */
    public Mono<NarrationResponse> requestNarration(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> findOwned(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(story -> {
                    if (story.getNarrationStatus() == NarrationStatus.READY) {
                        return Mono.just(new NarrationResponse("READY", audioStorage.urlFor(story.getNarrationKey())));
                    }
                    return Mono.fromCallable(() -> repository.claimNarration(id))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnNext(claimed -> {
                                if (claimed == 1) {
                                    synthesizeAndStore(story).subscribe();
                                }
                            })
                            .thenReturn(new NarrationResponse("GENERATING", null));
                });
    }

    /** GET: status poll. Returns the URL only once READY. */
    public Mono<NarrationResponse> getNarration(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> findOwned(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(story -> {
                    NarrationStatus status = story.getNarrationStatus();
                    String url = status == NarrationStatus.READY
                            ? audioStorage.urlFor(story.getNarrationKey()) : null;
                    return new NarrationResponse(status.name(), url);
                });
    }

    private Mono<Void> synthesizeAndStore(Story story) {
        return ttsClient.synthesize(story.getContent(), story.getLanguage())
                .flatMap(audio -> Mono.fromCallable(() ->
                                audioStorage.storeNarration(story.getId(), audio.bytes(),
                                        audio.contentType(), audio.fileExtension()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(key -> Mono.fromCallable(() -> repository.markNarrationReady(story.getId(), key))
                        .subscribeOn(Schedulers.boundedElastic()))
                .then()
                .onErrorResume(error -> {
                    log.warn("narration synthesis failed for story {}: {}", story.getId(), error.getMessage());
                    return Mono.fromCallable(() -> repository.markNarrationFailed(story.getId()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                });
    }

    /** Admin-aware lookup: admins may narrate any tale; users only their own. Mirrors StoryService.findOwned. */
    private Story findOwned(String id, CurrentUser currentUser) {
        var opt = currentUser.isAdmin()
                ? repository.findById(id)
                : repository.findByIdAndUserId(id, currentUser.userId());
        return opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
