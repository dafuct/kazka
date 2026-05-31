package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config slot for LLM + image providers. Text/editor/scene/judge use Google Gemini 2.5 Flash
 * via the OpenAI-compatible endpoint; images use Fal.ai FLUX.1-schnell. Migrated off the
 * HuggingFace Inference Router on 2026-05-30 —
 * see wiki/lessons/hf-router-strict-mode-rejects-repetition-penalty.md.
 */
@ConfigurationProperties("kazka.ai")
public class AiProviderProperties {

    private String apiToken = null;        // Gemini key (GOOGLE_API_KEY env slot)
    private String imageApiToken = null;   // Fal.ai key (FAL_KEY env slot)
    private String textModel = "gemini-2.5-flash";
    private String editorModel = "gemini-2.5-flash";
    private String imageModel = "fal-ai/flux/schnell";
    private String sceneModel = "gemini-2.5-flash";
    private String textBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";
    private String imageBaseUrl = "https://fal.run";
    // Fal's FLUX safety classifier false-positives heavily on whimsical/dreamlike children's
    // illustrations — verified locally with a "friendly fox in starry forest" prompt that came
    // back as a placeholder. Kazka already moderates the image scene prompt before it reaches
    // Fal (see ModerationJudgeClient + IllustrationService.chooseSafeScene), so disabling Fal's
    // output checker is safe. Override via FAL_SAFETY_CHECKER=true if input moderation is
    // ever disabled.
    private boolean imageSafetyChecker = false;

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

    public String getImageApiToken() { return imageApiToken; }
    public void setImageApiToken(String imageApiToken) { this.imageApiToken = imageApiToken; }

    public String getTextModel() { return textModel; }
    public void setTextModel(String textModel) { this.textModel = textModel; }

    public String getEditorModel() { return editorModel; }
    public void setEditorModel(String editorModel) { this.editorModel = editorModel; }

    public String getImageModel() { return imageModel; }
    public void setImageModel(String imageModel) { this.imageModel = imageModel; }

    public String getSceneModel() { return sceneModel; }
    public void setSceneModel(String sceneModel) { this.sceneModel = sceneModel; }

    public String getTextBaseUrl() { return textBaseUrl; }
    public void setTextBaseUrl(String textBaseUrl) { this.textBaseUrl = textBaseUrl; }

    public String getImageBaseUrl() { return imageBaseUrl; }
    public void setImageBaseUrl(String imageBaseUrl) { this.imageBaseUrl = imageBaseUrl; }

    public boolean isImageSafetyChecker() { return imageSafetyChecker; }
    public void setImageSafetyChecker(boolean imageSafetyChecker) { this.imageSafetyChecker = imageSafetyChecker; }

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
}
