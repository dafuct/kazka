package com.kazka.billing.gift.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record RedemptionResultDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int durationDays
) {}
