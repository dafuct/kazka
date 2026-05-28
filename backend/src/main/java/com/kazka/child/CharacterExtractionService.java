package com.kazka.child;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.child.dto.ExtractedCandidateDto;
import com.kazka.hf.HuggingFaceClient;
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

    private final HuggingFaceClient hfClient;
    private final String systemPrompt;

    public CharacterExtractionService(HuggingFaceClient hfClient) {
        this.hfClient = hfClient;
        this.systemPrompt = loadPrompt();
    }

    private static String loadPrompt() {
        try {
            String sys = new ClassPathResource("prompts/character-extraction-system.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            String fewshot = new ClassPathResource("prompts/character-extraction-fewshot.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            return sys.strip() + "\n\n---\n\n" + fewshot.strip();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load extraction prompt", e);
        }
    }

    public Mono<List<ExtractedCandidateDto>> extract(String storyBody) {
        if (storyBody == null || storyBody.isBlank()) return Mono.just(List.of());
        return hfClient.streamText(systemPrompt, storyBody)
                .reduce("", String::concat)
                .map(this::parse)
                .onErrorResume(e -> {
                    log.warn("Extraction LLM call failed: {}", e.getMessage());
                    return Mono.just(List.of());
                });
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
        } catch (Exception e) {
            log.warn("Failed to parse extraction JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> readTraits(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> traits = new ArrayList<>();
        for (JsonNode t : node) {
            if (traits.size() >= 8) break;
            String s = t.asText("").trim();
            if (!s.isEmpty()) traits.add(s);
        }
        return traits;
    }

    private String normalizeKind(String k) {
        return switch (k == null ? "" : k.toLowerCase()) {
            case "boy", "girl", "animal", "creature", "object" -> k.toLowerCase();
            default -> "object";
        };
    }

    private String normalizeRole(String r) {
        return switch (r == null ? "" : r.toLowerCase()) {
            case "protagonist", "companion", "mentioned" -> r.toLowerCase();
            default -> "companion";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
