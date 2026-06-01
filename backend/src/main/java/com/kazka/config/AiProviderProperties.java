package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config slot for LLM + image providers. Text/editor/scene/judge use Google Gemini 2.5 Flash
 * via the OpenAI-compatible endpoint; comics panels use Google's native Nano Banana endpoint
 * (gemini-2.5-flash-image). Both share the single {@code GOOGLE_API_KEY}. Migrated off the
 * HuggingFace Inference Router on 2026-05-30 (see
 * wiki/lessons/hf-router-strict-mode-rejects-repetition-penalty.md) and off Fal.ai on
 * 2026-05-31 when comics replaced single-cover illustrations.
 */
@ConfigurationProperties("kazka.ai")
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

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public String getTextModel() { return textModel; }
    public void setTextModel(String textModel) { this.textModel = textModel; }

    public String getEditorModel() { return editorModel; }
    public void setEditorModel(String editorModel) { this.editorModel = editorModel; }

    public String getSceneModel() { return sceneModel; }
    public void setSceneModel(String sceneModel) { this.sceneModel = sceneModel; }

    public String getTextBaseUrl() { return textBaseUrl; }
    public void setTextBaseUrl(String textBaseUrl) { this.textBaseUrl = textBaseUrl; }

    public String getNanoBananaModel() { return nanoBananaModel; }
    public void setNanoBananaModel(String nanoBananaModel) { this.nanoBananaModel = nanoBananaModel; }

    public String getNanoBananaBaseUrl() { return nanoBananaBaseUrl; }
    public void setNanoBananaBaseUrl(String nanoBananaBaseUrl) { this.nanoBananaBaseUrl = nanoBananaBaseUrl; }

    public double getTextTemperature() { return textTemperature; }
    public void setTextTemperature(double textTemperature) { this.textTemperature = textTemperature; }

    public double getTextTopP() { return textTopP; }
    public void setTextTopP(double textTopP) { this.textTopP = textTopP; }

    public double getTextFrequencyPenalty() { return textFrequencyPenalty; }
    public void setTextFrequencyPenalty(double textFrequencyPenalty) { this.textFrequencyPenalty = textFrequencyPenalty; }

    public double getTextPresencePenalty() { return textPresencePenalty; }
    public void setTextPresencePenalty(double textPresencePenalty) { this.textPresencePenalty = textPresencePenalty; }

    public int getTextMaxTokens() { return textMaxTokens; }
    public void setTextMaxTokens(int textMaxTokens) { this.textMaxTokens = textMaxTokens; }

    public double getEditorTemperature() { return editorTemperature; }
    public void setEditorTemperature(double editorTemperature) { this.editorTemperature = editorTemperature; }

    public double getEditorTopP() { return editorTopP; }
    public void setEditorTopP(double editorTopP) { this.editorTopP = editorTopP; }

    public double getEditorFrequencyPenalty() { return editorFrequencyPenalty; }
    public void setEditorFrequencyPenalty(double editorFrequencyPenalty) { this.editorFrequencyPenalty = editorFrequencyPenalty; }

    public double getEditorPresencePenalty() { return editorPresencePenalty; }
    public void setEditorPresencePenalty(double editorPresencePenalty) { this.editorPresencePenalty = editorPresencePenalty; }

    public int getEditorMaxTokens() { return editorMaxTokens; }
    public void setEditorMaxTokens(int editorMaxTokens) { this.editorMaxTokens = editorMaxTokens; }

    // --- Comics pipeline config (panels per tale, aspects, timeout) ---
    private final Comics comics = new Comics();
    public Comics getComics() { return comics; }

    public static class Comics {
        private int panelsPerTale = 4;
        private java.util.List<PanelAspectName> panelAspects =
            java.util.List.of(PanelAspectName.LANDSCAPE, PanelAspectName.SQUARE,
                              PanelAspectName.SQUARE, PanelAspectName.LANDSCAPE);
        private java.time.Duration pipelineTimeout = java.time.Duration.ofSeconds(60);
        private int maxConcurrentPerUser = 1;

        public int getPanelsPerTale() { return panelsPerTale; }
        public void setPanelsPerTale(int v) { this.panelsPerTale = v; }

        public java.util.List<PanelAspectName> getPanelAspects() { return panelAspects; }
        public void setPanelAspects(java.util.List<PanelAspectName> v) { this.panelAspects = v; }

        public java.time.Duration getPipelineTimeout() { return pipelineTimeout; }
        public void setPipelineTimeout(java.time.Duration v) { this.pipelineTimeout = v; }

        public int getMaxConcurrentPerUser() { return maxConcurrentPerUser; }
        public void setMaxConcurrentPerUser(int v) { this.maxConcurrentPerUser = v; }
    }

    /** Lower-cased mirror of com.kazka.comics.PanelAspect for property binding. */
    public enum PanelAspectName { LANDSCAPE, SQUARE }
}
