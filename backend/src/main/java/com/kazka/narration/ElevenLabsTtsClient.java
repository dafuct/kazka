package com.kazka.narration;

import com.kazka.config.AiProviderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin client over the ElevenLabs Text-to-Speech API ({@code POST /v1/text-to-speech/{voiceId}}).
 * Auth via the {@code xi-api-key} default header on the injected WebClient; the voice is chosen
 * per tale language from {@code kazka.ai.elevenlabs.voices}. Returns MP3 audio directly (the API
 * responds with raw audio bytes, not JSON), so no {@link WavEncoder} step is needed.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kazka.ai.tts-provider", havingValue = "elevenlabs", matchIfMissing = true)
public class ElevenLabsTtsClient implements TtsClient {

    private final AiProviderProperties props;
    private final WebClient elevenLabsWebClient;

    public ElevenLabsTtsClient(AiProviderProperties props,
                               @Qualifier("elevenLabsWebClient") WebClient elevenLabsWebClient) {
        this.props = props;
        this.elevenLabsWebClient = elevenLabsWebClient;
    }

    @Override
    public Mono<TtsAudio> synthesize(String text, String language) {
        AiProviderProperties.ElevenLabs el = props.getElevenlabs();
        String voiceId = el.voiceFor(language);
        if (voiceId == null || voiceId.isBlank()) {
            return Mono.error(new IllegalStateException(
                    "No ElevenLabs voice configured for language '" + language
                            + "' (set kazka.ai.elevenlabs.voices)"));
        }

        AiProviderProperties.VoiceSettings vs = el.getVoiceSettings();
        Map<String, Object> voiceSettings = new LinkedHashMap<>();
        voiceSettings.put("stability", vs.getStability());
        voiceSettings.put("similarity_boost", vs.getSimilarityBoost());
        voiceSettings.put("style", vs.getStyle());
        voiceSettings.put("speed", vs.getSpeed());
        voiceSettings.put("use_speaker_boost", vs.isUseSpeakerBoost());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        body.put("model_id", el.getModel());
        body.put("voice_settings", voiceSettings);

        return elevenLabsWebClient.post()
                .uri(uri -> uri.path("/v1/text-to-speech/{voiceId}")
                        .queryParam("output_format", el.getOutputFormat())
                        .build(voiceId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnError(e -> {
                    // On HTTP errors the message only carries the status line — the real cause
                    // (401 missing_permissions / 422 bad voice / 429 quota) is in the body.
                    if (e instanceof WebClientResponseException httpError) {
                        log.warn("elevenLabs.synthesize failed (model={}, status={}): {}",
                                el.getModel(), httpError.getStatusCode(), httpError.getResponseBodyAsString());
                    } else {
                        log.warn("elevenLabs.synthesize failed (model={}): {}", el.getModel(), e.getMessage());
                    }
                })
                // output-format is mp3_* (see AiProviderProperties default) → MP3 container.
                .map(bytes -> new TtsAudio(bytes, "audio/mpeg", "mp3"));
    }
}
