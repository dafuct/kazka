package com.kazka.story.branching.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record BranchingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String storyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int segmentNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(nullable = true) List<BranchingChoice> choices,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String branchingState,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean isFinal
) {}
