package com.kazka.holidays.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record HolidayDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String suggestedTheme,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant date
) {}
