package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kazka.huggingface")
public class HuggingFaceProperties {

    private String apiToken = null;
    private String textModel = "meta-llama/Llama-3.3-70B-Instruct";
    private String editorModel = "meta-llama/Llama-3.3-70B-Instruct";
    private String imageModel = "black-forest-labs/FLUX.1-schnell";
    private String sceneModel = "Qwen/Qwen3-32B";
    private String textBaseUrl = "https://router.huggingface.co";
    private String imageBaseUrl = "https://router.huggingface.co";

    private double textTemperature = 0.75;
    private double textTopP = 0.9;
    private double textRepetitionPenalty = 1.0;
    private int textMaxTokens = 4096;

    private double editorTemperature = 0.3;
    private double editorTopP = 0.9;
    private double editorRepetitionPenalty = 1.0;
    private int editorMaxTokens = 4096;

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

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

    public double getTextTemperature() { return textTemperature; }
    public void setTextTemperature(double textTemperature) { this.textTemperature = textTemperature; }

    public double getTextTopP() { return textTopP; }
    public void setTextTopP(double textTopP) { this.textTopP = textTopP; }

    public double getTextRepetitionPenalty() { return textRepetitionPenalty; }
    public void setTextRepetitionPenalty(double textRepetitionPenalty) { this.textRepetitionPenalty = textRepetitionPenalty; }

    public int getTextMaxTokens() { return textMaxTokens; }
    public void setTextMaxTokens(int textMaxTokens) { this.textMaxTokens = textMaxTokens; }

    public double getEditorTemperature() { return editorTemperature; }
    public void setEditorTemperature(double editorTemperature) { this.editorTemperature = editorTemperature; }

    public double getEditorTopP() { return editorTopP; }
    public void setEditorTopP(double editorTopP) { this.editorTopP = editorTopP; }

    public double getEditorRepetitionPenalty() { return editorRepetitionPenalty; }
    public void setEditorRepetitionPenalty(double editorRepetitionPenalty) { this.editorRepetitionPenalty = editorRepetitionPenalty; }

    public int getEditorMaxTokens() { return editorMaxTokens; }
    public void setEditorMaxTokens(int editorMaxTokens) { this.editorMaxTokens = editorMaxTokens; }
}
