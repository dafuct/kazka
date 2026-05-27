package com.kazka.story.branching.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BranchingChoice(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String text
) {}
