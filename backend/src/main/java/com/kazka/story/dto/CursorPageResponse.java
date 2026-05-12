package com.kazka.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CursorPageResponse<T>(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> items,
        @Schema(nullable = true) String nextCursor
) {}
