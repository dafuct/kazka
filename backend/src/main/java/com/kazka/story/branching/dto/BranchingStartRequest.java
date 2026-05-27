package com.kazka.story.branching.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;

public record BranchingStartRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String theme,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty List<String> characters,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Pattern(regexp = "3-5|6-8|9-12") String ageGroup,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Pattern(regexp = "short|medium|long") String length,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Pattern(regexp = "uk|en") String language,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String childProfileId,
        @Schema(nullable = true) @Size(max = 3) List<String> includeCharacterIds
) {}
