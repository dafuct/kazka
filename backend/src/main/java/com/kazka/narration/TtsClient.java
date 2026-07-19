package com.kazka.narration;

import reactor.core.publisher.Mono;

/**
 * Synthesizes read-aloud narration for a tale. Implementations own their own voice selection
 * (per tale language) and output container, so {@link NarrationService} stays provider-agnostic.
 * The active implementation is selected by {@code kazka.ai.tts-provider} (elevenlabs | gemini).
 */
public interface TtsClient {

    /**
     * Synthesize browser-playable audio for {@code text} in the tale's {@code language}
     * (ISO 639-1, e.g. {@code "uk"} / {@code "en"}). The implementation chooses the voice.
     */
    Mono<TtsAudio> synthesize(String text, String language);
}
