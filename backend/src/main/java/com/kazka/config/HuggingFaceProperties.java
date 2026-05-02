package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kazka.huggingface")
public class HuggingFaceProperties {

    private String apiToken = null;
    private String textModel = "Qwen/Qwen2.5-72B-Instruct";
    private String editorModel = "Qwen/Qwen2.5-72B-Instruct";
    private String imageModel = "black-forest-labs/FLUX.1-schnell";
    private String sceneModel = "Qwen/Qwen2.5-72B-Instruct";
    private String textBaseUrl = "https://router.huggingface.co";
    private String imageBaseUrl = "https://router.huggingface.co";

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
}
