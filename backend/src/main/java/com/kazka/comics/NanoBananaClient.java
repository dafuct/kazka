package com.kazka.comics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.config.AiProviderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client over Google Gemini's native image-generation endpoint
 * (gemini-2.5-flash-image, a.k.a. "Nano Banana"). Auth via x-goog-api-key
 * (the WebClient bean already sets it as a default header).
 *
 * The comic page is rendered in a single call ({@code priorPng == null}); a
 * whole multi-panel page in one image keeps the character consistent on its
 * own. The {@code priorPng} parameter still supports image-conditioning (a
 * prior PNG passed as an inline_data part) should a chained flow ever need it.
 */
@Slf4j
@Component
public class NanoBananaClient {

    private final AiProviderProperties props;
    private final WebClient nanoBananaWebClient;
    private final ObjectMapper mapper;

    /**
     * Constructor parameter is named {@code nanoBananaWebClient} on purpose: Spring resolves
     * by-type first, then falls back to by-name when multiple {@link WebClient} beans exist
     * ({@code textClient}, {@code nanoBananaWebClient}, {@code judgeWebClient}). The bean is
     * named with a {@code WebClient} suffix to avoid collision with this {@code @Component}'s
     * own bean name ({@code nanoBananaClient}).
     */
    public NanoBananaClient(AiProviderProperties props,
                            WebClient nanoBananaWebClient,
                            ObjectMapper mapper) {
        this.props = props;
        this.nanoBananaWebClient = nanoBananaWebClient;
        this.mapper = mapper;
        if (props.getApiToken() == null || props.getApiToken().isBlank()) {
            log.warn("kazka.ai.api-token is not set — Nano Banana calls will fail with 401");
        }
    }

    /**
     * Generate one panel image.
     *
     * @param scenePrompt full text prompt for this panel (scene + aesthetic instructions)
     * @param aspect      LANDSCAPE (16:9), SQUARE (1:1), or PAGE (3:4)
     * @param priorPng    bytes to condition on as inline_data, or null (single-page render passes null)
     * @return PNG bytes of the generated image
     */
    public Mono<byte[]> generate(String scenePrompt, PanelAspect aspect, byte[] priorPng) {
        List<Map<String, Object>> parts = new ArrayList<>(List.of(Map.of("text", scenePrompt)));
        if (priorPng != null && priorPng.length > 0) {
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", "image/png",
                    "data", Base64.getEncoder().encodeToString(priorPng))));
        }
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("response_modalities", List.of("TEXT", "IMAGE"));
        generationConfig.put("image_config", Map.of("aspect_ratio", aspect.aspectRatio()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", parts)));
        body.put("generationConfig", generationConfig);

        return nanoBananaWebClient.post()
                .uri("/models/{model}:generateContent", props.getNanoBananaModel())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.warn("nanoBanana.generate failed (model={}): {}",
                        props.getNanoBananaModel(), e.getMessage()))
                .map(this::extractFirstImage);
    }

    private byte[] extractFirstImage(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    // The API may return inline_data (snake) or inlineData (camel) depending on transport — handle both.
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
            throw new IllegalStateException("Nano Banana response had no inline image part. Body head: " + snippet);
        } catch (com.fasterxml.jackson.core.JsonProcessingException jsonException) {
            throw new IllegalStateException("Nano Banana returned invalid JSON: " + jsonException.getMessage(), jsonException);
        }
    }
}
