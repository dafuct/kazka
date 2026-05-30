package com.kazka.moderation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("kazka.moderation")
public class ModerationProperties {

    private String judgeModel = "gemini-2.5-flash";
    private String judgeBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";
    private Duration judgeTimeout = Duration.ofSeconds(5);
    private int suspensionThreshold = 3;
    private Duration suspensionWindow = Duration.ofHours(24);
    private int retentionDays = 90;
    private Duration cacheTtl = Duration.ofHours(1);
    private String safeFallbackScene = "two friends in a sunlit forest at sunset";

    public String getJudgeModel() { return judgeModel; }
    public void setJudgeModel(String judgeModel) { this.judgeModel = judgeModel; }
    public String getJudgeBaseUrl() { return judgeBaseUrl; }
    public void setJudgeBaseUrl(String judgeBaseUrl) { this.judgeBaseUrl = judgeBaseUrl; }
    public Duration getJudgeTimeout() { return judgeTimeout; }
    public void setJudgeTimeout(Duration judgeTimeout) { this.judgeTimeout = judgeTimeout; }
    public int getSuspensionThreshold() { return suspensionThreshold; }
    public void setSuspensionThreshold(int suspensionThreshold) { this.suspensionThreshold = suspensionThreshold; }
    public Duration getSuspensionWindow() { return suspensionWindow; }
    public void setSuspensionWindow(Duration suspensionWindow) { this.suspensionWindow = suspensionWindow; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
    public String getSafeFallbackScene() { return safeFallbackScene; }
    public void setSafeFallbackScene(String safeFallbackScene) { this.safeFallbackScene = safeFallbackScene; }
}
