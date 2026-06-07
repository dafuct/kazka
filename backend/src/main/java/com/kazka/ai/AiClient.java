package com.kazka.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.config.AiProviderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin OpenAI-compatible chat client for text/editor/scene Gemini calls.
 * Image generation lives in {@link com.kazka.comics.NanoBananaClient}; this class no longer handles images.
 * Migrated off the HuggingFace Inference Router on 2026-05-30 —
 * see wiki/lessons/hf-router-strict-mode-rejects-repetition-penalty.md for the why.
 */
@Slf4j
@Component
public class AiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient textClient;
    private final AiProviderProperties props;

    public AiClient(AiProviderProperties props, WebClient textClient) {
        this.props = props;
        if (props.getApiToken() == null || props.getApiToken().isBlank()) {
            log.warn("kazka.ai.api-token is not set — chat-completion calls will fail with 401");
        }
        log.info("LLM models resolved at startup: text={}, editor={}, scene={}",
                props.getTextModel(), props.getEditorModel(), props.getSceneModel());
        this.textClient = textClient;
    }

    public Flux<String> streamText(String system, String user) {
        return streamRequest(props.getTextModel(), system, user,
                props.getTextTemperature(), props.getTextTopP(),
                props.getTextFrequencyPenalty(), props.getTextPresencePenalty(),
                props.getTextMaxTokens());
    }

    public Flux<String> streamEdit(String system, String user) {
        return streamRequest(props.getEditorModel(), system, user,
                props.getEditorTemperature(), props.getEditorTopP(),
                props.getEditorFrequencyPenalty(), props.getEditorPresencePenalty(),
                props.getEditorMaxTokens());
    }

    public Mono<String> generateText(String system, String user) {
        // `reasoning_effort: "none"` disables Gemini 2.5's chain-of-thought. Without it,
        // thinking can consume most of the max_tokens budget and the visible JSON output
        // gets truncated mid-field (see ModerationJudgeClient for the same fix). This is
        // a structured-output path (ActsStructurer parses a 5-beat JSON array), so thinking
        // adds no value but breaks reliability.
        return textClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", props.getSceneModel(),
                        "messages", List.of(
                                Map.of("role", "system", "content", system),
                                Map.of("role", "user", "content", user)
                        ),
                        "stream", false,
                        "max_tokens", 4096,
                        "reasoning_effort", "none"
                ))
                .retrieve()
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .doOnError(e -> log.warn("generateText failed (model={}): {}", props.getSceneModel(), e.getMessage()))
                .map(AiClient::extractChatContent);
    }

    private static String extractChatContent(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            JsonNode node = MAPPER.readTree(body);
            return node.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception exception) {
            log.warn("Could not parse chat response body (first 200 chars): {}",
                    body.substring(0, Math.min(200, body.length())));
            return "";
        }
    }

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private Flux<String> streamRequest(String model, String system, String user,
                                        double temperature, double topP,
                                        double frequencyPenalty, double presencePenalty,
                                        int maxTokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        ));
        body.put("stream", true);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("top_p", topP);
        if (frequencyPenalty != 0.0) body.put("frequency_penalty", frequencyPenalty);
        if (presencePenalty != 0.0) body.put("presence_penalty", presencePenalty);
        // Use Spring's ServerSentEvent codec rather than `bodyToFlux(String.class)`. The latter
        // emits one String per network buffer, which can carry multiple SSE events glued together
        // or a partial event split across buffers — both of which cause silent token drops on the
        // Gemini stream (Gemini batches several tokens into each `data:` event, so each dropped
        // buffer loses a noticeable chunk of the tale).
        return textClient.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(SSE_TYPE)
                .map(ServerSentEvent::data)
                .takeWhile(data -> !"[DONE]".equals(data))
                .flatMap(data -> {
                    if (data == null || data.isBlank()) return Flux.empty();
                    try {
                        JsonNode node = MAPPER.readTree(data);
                        String token = node.path("choices").path(0)
                                .path("delta").path("content").asText("");
                        return token.isEmpty() ? Flux.empty() : Flux.just(token);
                    } catch (Exception exception) {
                        log.warn("SSE chunk parse failed (first 200 chars): {}",
                                data.substring(0, Math.min(200, data.length())));
                        return Flux.empty();
                    }
                });
    }

}
