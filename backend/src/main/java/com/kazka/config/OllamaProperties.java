package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kazka.ollama")
public class OllamaProperties {
    private String baseUrl = "http://localhost:11434";
    private String textModel = "gemma3:4b";
    private String imageModel = "x/flux2-klein";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getTextModel() { return textModel; }
    public void setTextModel(String textModel) { this.textModel = textModel; }

    public String getImageModel() { return imageModel; }
    public void setImageModel(String imageModel) { this.imageModel = imageModel; }
}
