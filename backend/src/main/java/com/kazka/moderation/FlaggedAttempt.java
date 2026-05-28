package com.kazka.moderation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "flagged_attempts")
@Getter
@Setter
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

    // Hibernate 7's @CreationTimestamp overwrites preset values; @PrePersist with a null-guard
    // lets tests backdate rows and admin-imported rows preserve their created_at.
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
