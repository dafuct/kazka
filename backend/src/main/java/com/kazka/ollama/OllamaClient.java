package com.kazka.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;

    public OllamaClient(WebClient ollamaWebClient) {
        this.webClient = ollamaWebClient;
    }

    public Flux<String> streamGenerate(String model, String prompt) {
        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("model", model, "prompt", prompt, "stream", true))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    try {
                        JsonNode node = MAPPER.readTree(line);
                        boolean done = node.path("done").asBoolean(false);
                        if (done) return Flux.empty();
                        String token = node.path("response").asText("");
                        if (token.isEmpty()) return Flux.empty();
                        return Flux.just(token);
                    } catch (Exception e) {
                        return Flux.empty();
                    }
                });
    }

    public Mono<String> generateImage(String model, String prompt) {
        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("model", model, "prompt", prompt, "stream", false))
                .retrieve()
                .bodyToMono(String.class)
                .mapNotNull(json -> {
                    try {
                        JsonNode node = MAPPER.readTree(json);
                        String img = node.path("images").path(0).asText("");
                        return img.isBlank() ? null : img;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Image generation failed: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    public Flux<String> pullModel(String model) {
        return webClient.post()
                .uri("/api/pull")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", model, "stream", true))
                .retrieve()
                .bodyToFlux(String.class)
                .onErrorResume(e -> {
                    log.warn("Model pull failed for {}: {}", model, e.getMessage());
                    return Flux.empty();
                });
    }
}
