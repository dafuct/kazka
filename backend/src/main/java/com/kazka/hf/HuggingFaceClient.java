package com.kazka.hf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.config.HuggingFaceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin OpenAI-compatible chat client (text/editor/scene) + Fal.ai image client.
 * Name retained for blast-radius reasons after the HF → Gemini/Fal migration
 * (wiki/lessons/hf-router-strict-mode-rejects-repetition-penalty.md).
 */
@Slf4j
@Component
public class HuggingFaceClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient textClient;
    private final WebClient imageClient;
    private final HuggingFaceProperties props;

    public HuggingFaceClient(HuggingFaceProperties props, WebClient textClient, WebClient imageClient) {
        this.props = props;
        if (props.getApiToken() == null || props.getApiToken().isBlank()) {
            log.warn("kazka.huggingface.api-token is not set — chat-completion calls will fail with 401");
        }
        if (props.getImageApiToken() == null || props.getImageApiToken().isBlank()) {
            log.warn("kazka.huggingface.image-api-token (FAL_KEY) is not set — image calls will fail with 401");
        }
        log.info("LLM models resolved at startup: text={}, editor={}, scene={}, image={}",
                props.getTextModel(), props.getEditorModel(), props.getSceneModel(), props.getImageModel());
        this.imageClient = imageClient;
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
                        "max_tokens", 4096
                ))
                .retrieve()
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .doOnError(e -> log.warn("generateText failed (model={}): {}", props.getSceneModel(), e.getMessage()))
                .map(HuggingFaceClient::extractChatContent);
    }

    private static String extractChatContent(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            JsonNode node = MAPPER.readTree(body);
            return node.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
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
                    } catch (Exception e) {
                        log.warn("SSE chunk parse failed (first 200 chars): {}",
                                data.substring(0, Math.min(200, data.length())));
                        return Flux.empty();
                    }
                });
    }

    public Mono<byte[]> generateImage(String prompt, int width, int height) {
        return generateImage(prompt, width, height, null);
    }

    /**
     * Generate one image via Fal.ai FLUX.1-schnell. Uses `sync_mode=true` so the response
     * carries the image inline as a data URI — saves a follow-up HTTP fetch.
     * Fal accepts preset image sizes only for this model, so (width,height) is mapped to
     * the closest preset; unknown ratios fall back to landscape_4_3 with a warning log.
     */
    public Mono<byte[]> generateImage(String prompt, int width, int height, Long seed) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", prompt);
        body.put("image_size", mapImageSize(width, height));
        body.put("num_inference_steps", 4);
        body.put("num_images", 1);
        body.put("enable_safety_checker", props.isImageSafetyChecker());
        body.put("output_format", "png");
        body.put("sync_mode", true);
        if (seed != null) body.put("seed", seed);

        return imageClient.post()
                .uri("/" + props.getImageModel())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.warn("generateImage failed (model={}): {}", props.getImageModel(), e.getMessage()))
                .map(HuggingFaceClient::extractImageBytes);
    }

    private static String mapImageSize(int width, int height) {
        if (width == 1024 && height == 768) return "landscape_4_3";
        if (width == 768 && height == 1024) return "portrait_4_3";
        if (width == 1024 && height == 1024) return "square_hd";
        if (width == height) return "square";
        if (width > height) {
            double r = (double) width / height;
            return r > 1.5 ? "landscape_16_9" : "landscape_4_3";
        }
        double r = (double) height / width;
        return r > 1.5 ? "portrait_16_9" : "portrait_4_3";
    }

    private static byte[] extractImageBytes(String body) {
        JsonNode response;
        try {
            response = MAPPER.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Fal response not valid JSON (first 200 chars): "
                    + body.substring(0, Math.min(200, body.length())), e);
        }
        JsonNode firstImage = response.path("images").path(0);
        String url = firstImage.path("url").asText("");
        if (url.isEmpty()) {
            throw new IllegalStateException("Fal response missing images[0].url");
        }
        // sync_mode=true returns a data URI like `data:image/png;base64,iVBORw0KGgo...`.
        // If a non-sync response slips through with a plain https URL, fail loudly — the
        // calling code expects bytes and there is no follow-up fetch wired up here yet.
        int comma = url.indexOf(',');
        if (!url.startsWith("data:") || comma < 0) {
            throw new IllegalStateException("Expected data: URI from Fal sync_mode=true, got: "
                    + url.substring(0, Math.min(64, url.length())));
        }
        return Base64.getDecoder().decode(url.substring(comma + 1));
    }
}
