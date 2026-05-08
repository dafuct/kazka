package com.kazka.moderation;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "flagged_attempts")
public class FlaggedAttempt {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationPipeline pipeline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ModerationCategory category;

    @Column(nullable = false, length = 5)
    private String language;

    @Column(name = "prompt_text", columnDefinition = "TEXT", nullable = false)
    private String promptText;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "judge_model", length = 100)
    private String judgeModel;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public ModerationPipeline getPipeline() { return pipeline; }
    public void setPipeline(ModerationPipeline pipeline) { this.pipeline = pipeline; }
    public ModerationCategory getCategory() { return category; }
    public void setCategory(ModerationCategory category) { this.category = category; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getJudgeModel() { return judgeModel; }
    public void setJudgeModel(String judgeModel) { this.judgeModel = judgeModel; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
