package com.kazka.hf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.kazka.config.HuggingFaceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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
            log.warn("kazka.huggingface.api-token is not set — HF API calls will fail with 401");
        }
        log.info("HF models resolved at startup: text={}, editor={}, scene={}, image={}",
                props.getTextModel(), props.getEditorModel(), props.getSceneModel(), props.getImageModel());
        this.imageClient = imageClient;
        this.textClient = textClient;
    }

    public Flux<String> streamText(String system, String user) {
        return streamRequest(props.getTextModel(), system, user,
                props.getTextTemperature(), props.getTextTopP(),
                props.getTextRepetitionPenalty(), props.getTextMaxTokens());
    }

    public Flux<String> streamEdit(String system, String user) {
        return streamRequest(props.getEditorModel(), system, user,
                props.getEditorTemperature(), props.getEditorTopP(),
                props.getEditorRepetitionPenalty(), props.getEditorMaxTokens());
    }

    public Mono<String> generateText(String system, String user) {
        return textClient.post()
                .uri("/v1/chat/completions")
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
                .bodyToMono(JsonNode.class)
                .defaultIfEmpty(NullNode.getInstance())
                .doOnError(e -> log.warn("generateText failed (model={}): {}", props.getSceneModel(), e.getMessage()))
                .map(node -> node.path("choices").path(0)
                        .path("message").path("content").asText(""));
    }

    private Flux<String> streamRequest(String model, String system, String user,
                                        double temperature, double topP,
                                        double repetitionPenalty, int maxTokens) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        ));
        body.put("stream", true);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("top_p", topP);
        // repetition_penalty is a Featherless/older-provider extension; Groq (which HF's router
        // sends Llama 3.3 to) returns 400 "property unsupported" if it's present. Send only when
        // explicitly enabled (>1.0) so swapping providers stays declarative.
        if (repetitionPenalty > 1.0) {
            body.put("repetition_penalty", repetitionPenalty);
        }
        return textClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) return Flux.empty();
                    String json = trimmed.startsWith("data:") ? trimmed.substring(5).trim() : trimmed;
                    if (json.equals("[DONE]")) return Flux.empty();
                    try {
                        JsonNode node = MAPPER.readTree(json);
                        String token = node.path("choices").path(0)
                                .path("delta").path("content").asText("");
                        return token.isEmpty() ? Flux.empty() : Flux.just(token);
                    } catch (Exception e) {
                        return Flux.empty();
                    }
                });
    }

    public Mono<byte[]> generateImage(String prompt, int width, int height) {
        return generateImage(prompt, width, height, null);
    }

    public Mono<byte[]> generateImage(String prompt, int width, int height, Long seed) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("width", width);
        params.put("height", height);
        if (seed != null) params.put("seed", seed);

        return imageClient.post()
                .uri("/hf-inference/models/" + props.getImageModel())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.IMAGE_PNG)
                .bodyValue(Map.of(
                        "inputs", prompt,
                        "parameters", params
                ))
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnError(e -> log.warn("generateImage failed (model={}): {}", props.getImageModel(), e.getMessage()));
    }

}
