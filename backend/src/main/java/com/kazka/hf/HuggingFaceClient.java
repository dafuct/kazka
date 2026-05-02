package com.kazka.hf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.kazka.config.HuggingFaceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class HuggingFaceClient {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient textClient;
    private final WebClient imageClient;
    private final HuggingFaceProperties props;

    public HuggingFaceClient(WebClient.Builder builder, HuggingFaceProperties props) {
        this.props = props;
        if (props.getApiToken() == null || props.getApiToken().isBlank()) {
            log.warn("kazka.huggingface.api-token is not set — HF API calls will fail with 401");
        }
        String auth = "Bearer " + props.getApiToken();
        this.textClient = builder.clone()
                .baseUrl(props.getTextBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, auth)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.imageClient = builder.clone()
                .baseUrl(props.getImageBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, auth)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    public Flux<String> streamText(String system, String user) {
        return streamRequest(props.getTextModel(), system, user);
    }

    public Flux<String> streamEdit(String system, String user) {
        return streamRequest(props.getEditorModel(), system, user);
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

    private Flux<String> streamRequest(String model, String system, String user) {
        return textClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", system),
                                Map.of("role", "user", "content", user)
                        ),
                        "stream", true,
                        "max_tokens", 4096
                ))
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
