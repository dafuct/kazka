package com.kazka.moderation;

import java.math.BigDecimal;
import java.time.Instant;

public record FlaggedAttemptDto(
        String id,
        String userId,
        String userEmail,
        ModerationPipeline pipeline,
        ModerationCategory category,
        String language,
        String promptText,
        BigDecimal confidence,
        String judgeModel,
        Instant createdAt
) {}
