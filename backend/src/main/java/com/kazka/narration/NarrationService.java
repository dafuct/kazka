package com.kazka.narration;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.config.AiProviderProperties;
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
 * synthesizes via Gemini TTS off the request thread (fire-and-forget), wraps to WAV, and caches it;
 * later requests return the cached presigned URL. Admins may narrate any tale (admin-aware lookup).
 */
@Slf4j
@Service
public class NarrationService {

    private final StoryRepository repository;
    private final GeminiTtsClient ttsClient;
    private final WavEncoder wavEncoder;
    private final AudioStorage audioStorage;
    private final AiProviderProperties props;

    public NarrationService(StoryRepository repository, GeminiTtsClient ttsClient,
                            WavEncoder wavEncoder, AudioStorage audioStorage,
                            AiProviderProperties props) {
        this.repository = repository;
        this.ttsClient = ttsClient;
        this.wavEncoder = wavEncoder;
        this.audioStorage = audioStorage;
        this.props = props;
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
        String text = props.getTtsStylePrompt() + "\n\n" + story.getContent();
        return ttsClient.synthesizePcm(text, props.getTtsVoice())
                .map(wavEncoder::wrap24kMono16)
                .flatMap(wav -> Mono.fromCallable(() -> audioStorage.storeNarration(story.getId(), wav))
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
