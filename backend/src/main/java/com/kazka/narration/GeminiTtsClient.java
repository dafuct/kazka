package com.kazka.narration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.config.AiProviderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client over Google Gemini's native TTS endpoint (gemini-2.5-flash-preview-tts).
 * Mirrors {@link com.kazka.comics.NanoBananaClient}: native v1beta {@code generateContent},
 * auth via the {@code x-goog-api-key} default header on the injected WebClient. Returns the
 * spoken audio as raw little-endian PCM (24 kHz, signed 16-bit, mono) — wrap with
 * {@link WavEncoder} before serving.
 */
@Slf4j
@Component
public class GeminiTtsClient {

    private final AiProviderProperties props;
    private final WebClient geminiTtsWebClient;
    private final ObjectMapper mapper;

    public GeminiTtsClient(AiProviderProperties props,
                           @Qualifier("geminiTtsWebClient") WebClient geminiTtsWebClient,
                           ObjectMapper mapper) {
        this.props = props;
        this.geminiTtsWebClient = geminiTtsWebClient;
        this.mapper = mapper;
        if (props.getApiToken() == null || props.getApiToken().isBlank()) {
            log.warn("kazka.ai.api-token is not set — Gemini TTS calls will fail with 401");
        }
    }

    /**
     * Synthesize speech for {@code text} using the named prebuilt voice.
     * @return raw PCM bytes (24 kHz / 16-bit / mono).
     */
    public Mono<byte[]> synthesizePcm(String text, String voiceName) {
        Map<String, Object> speechConfig = Map.of(
                "voice_config", Map.of(
                        "prebuilt_voice_config", Map.of("voice_name", voiceName)));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("response_modalities", List.of("AUDIO"));
        generationConfig.put("speech_config", speechConfig);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", text)))));
        body.put("generationConfig", generationConfig);

        return geminiTtsWebClient.post()
                .uri("/models/{model}:generateContent", props.getTtsModel())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.warn("geminiTts.synthesize failed (model={}): {}",
                        props.getTtsModel(), e.getMessage()))
                .map(this::extractFirstAudio);
    }

    private byte[] extractFirstAudio(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    // Snake (inline_data) or camel (inlineData) depending on transport — handle both.
                    JsonNode inline = part.path("inline_data");
                    if (inline.isMissingNode()) inline = part.path("inlineData");
                    if (!inline.isMissingNode()) {
                        String data = inline.path("data").asText("");
                        if (!data.isEmpty()) {
                            return Base64.getDecoder().decode(data);
                        }
                    }
                }
            }
            String snippet = responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
            throw new IllegalStateException("Gemini TTS response had no inline audio part. Body head: " + snippet);
        } catch (com.fasterxml.jackson.core.JsonProcessingException jsonException) {
            throw new IllegalStateException("Gemini TTS returned invalid JSON: " + jsonException.getMessage(), jsonException);
        }
    }
}
