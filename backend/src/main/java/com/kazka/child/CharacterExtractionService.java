package com.kazka.child;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.child.dto.ExtractedCandidateDto;
import com.kazka.ai.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CharacterExtractionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiClient aiClient;
    private final String systemPrompt;

    public CharacterExtractionService(AiClient aiClient) {
        this.aiClient = aiClient;
        this.systemPrompt = loadPrompt();
    }

    private static String loadPrompt() {
        try {
            String sys = new ClassPathResource("prompts/character-extraction-system.md")
                    .getContentAsString(StandardCharsets.UTF_8);
            String fewshot = new ClassPathResource("prompts/character-extraction-fewshot.md")
                    .getContentAsString(StandardCharsets.UTF_8);
            return sys.strip() + "\n\n---\n\n" + fewshot.strip();
        } catch (IOException ioException) {
            throw new IllegalStateException("Cannot load extraction prompt", ioException);
        }
    }

    public Mono<List<ExtractedCandidateDto>> extract(String storyBody, String language) {
        if (storyBody == null || storyBody.isBlank()) return Mono.just(List.of());
        String langName = "uk".equalsIgnoreCase(language) ? "Ukrainian" : "English";
        String sys = systemPrompt + "\n\nWrite the 'description' field in " + langName
                + ". Keep proper names ('name' field) as they appear in the tale.";
        return aiClient.streamText(sys, storyBody)
                .reduce("", String::concat)
                .map(this::parse)
                .onErrorResume(e -> {
                    log.warn("Extraction LLM call failed: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    public Mono<List<ExtractedCandidateDto>> extract(String storyBody) {
        return extract(storyBody, "en");
    }

    private List<ExtractedCandidateDto> parse(String raw) {
        if (raw == null) return List.of();
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }
        try {
            JsonNode array = MAPPER.readTree(cleaned);
            if (!array.isArray()) return List.of();
            List<ExtractedCandidateDto> out = new ArrayList<>();
            for (JsonNode node : array) {
                if (out.size() >= 6) break;
                out.add(new ExtractedCandidateDto(
                        node.path("name").asText(""),
                        normalizeKind(node.path("kind").asText("object")),
                        truncate(node.path("description").asText(""), 280),
                        readTraits(node.path("traits")),
                        normalizeRole(node.path("role").asText("companion"))
                ));
            }
            return out;
        } catch (Exception exception) {
            log.warn("Failed to parse extraction JSON: {}", exception.getMessage());
            return List.of();
        }
    }

    private List<String> readTraits(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> traits = new ArrayList<>();
        for (JsonNode t : node) {
            if (traits.size() >= 8) break;
            String trait = t.asText("").trim();
            if (!trait.isEmpty()) traits.add(trait);
        }
        return traits;
    }

    private String normalizeKind(String kind) {
        return switch (kind == null ? "" : kind.toLowerCase()) {
            case "boy", "girl", "animal", "creature", "object" -> kind.toLowerCase();
            default -> "object";
        };
    }

    private String normalizeRole(String role) {
        return switch (role == null ? "" : role.toLowerCase()) {
            case "protagonist", "companion", "mentioned" -> role.toLowerCase();
            default -> "companion";
        };
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) : text;
    }
}
