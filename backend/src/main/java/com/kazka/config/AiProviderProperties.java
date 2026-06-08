package com.kazka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Config slot for LLM + image providers. Text/editor/scene/judge use Google Gemini 2.5 Flash
 * via the OpenAI-compatible endpoint; comics panels use Google's native Nano Banana endpoint
 * (gemini-2.5-flash-image). Both share the single {@code GOOGLE_API_KEY}. Migrated off the
 * HuggingFace Inference Router on 2026-05-30 (see
 * wiki/lessons/hf-router-strict-mode-rejects-repetition-penalty.md) and off Fal.ai on
 * 2026-05-31 when comics replaced single-cover illustrations.
 */
@ConfigurationProperties("kazka.ai")
@Getter
@Setter
public class AiProviderProperties {

    private String apiToken = null;        // Gemini key (GOOGLE_API_KEY env slot)
    private String textModel = "gemini-2.5-flash";
    private String editorModel = "gemini-2.5-flash";
    private String sceneModel = "gemini-2.5-flash";
    private String textBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";

    // Nano Banana (Gemini 2.5 Flash Image) — Google's native multimodal image endpoint.
    // Auth via x-goog-api-key header (NOT Authorization: Bearer); reuses GOOGLE_API_KEY.
    private String nanoBananaModel = "gemini-2.5-flash-image";
    private String nanoBananaBaseUrl = "https://generativelanguage.googleapis.com/v1beta";

    private double textTemperature = 0.75;
    private double textTopP = 0.9;
    // Gemini's OpenAI-compat endpoint rejects frequency_penalty/presence_penalty with 400.
    // Defaults are 0.0 so the client skips sending them. Override via env only if the runtime
    // endpoint is OpenAI/OpenRouter/Together — those DO accept the OpenAI-standard penalties.
    private double textFrequencyPenalty = 0.0;
    private double textPresencePenalty = 0.0;
    private int textMaxTokens = 4096;

    private double editorTemperature = 0.3;
    private double editorTopP = 0.9;
    private double editorFrequencyPenalty = 0.0;
    private double editorPresencePenalty = 0.0;
    private int editorMaxTokens = 4096;

    // --- Comics pipeline config (panels per tale, aspects, timeout) ---
    private final Comics comics = new Comics();

    @Getter
    @Setter
    public static class Comics {
        private int panelsPerTale = 4;
        private List<PanelAspectName> panelAspects =
            List.of(PanelAspectName.LANDSCAPE, PanelAspectName.SQUARE,
                              PanelAspectName.SQUARE, PanelAspectName.LANDSCAPE);
        private Duration pipelineTimeout = Duration.ofSeconds(60);
        private int maxConcurrentPerUser = 1;
    }

    /** Lower-cased mirror of com.kazka.comics.PanelAspect for property binding. */
    public enum PanelAspectName { LANDSCAPE, SQUARE }
}
