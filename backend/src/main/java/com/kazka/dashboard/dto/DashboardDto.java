package com.kazka.dashboard.dto;

import com.kazka.story.dto.StoryDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record DashboardDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Aggregates aggregates,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ChildSummary> children,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<StoryDto> recentTales,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean isPro
) {
    public record Aggregates(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long talesTotal,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long talesThisWeek,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long talesThisMonth
    ) {}

    public record ChildSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String childProfileId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long taleCount,
            @Schema(nullable = true) StoryDto latestTale,
            @Schema(nullable = true) Instant lastBedtimeAt
    ) {}
}
