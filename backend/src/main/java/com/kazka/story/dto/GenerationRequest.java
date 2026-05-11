package com.kazka.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record GenerationRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String theme,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty List<String> characters,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"3-5", "6-8", "9-12"})
            @Pattern(regexp = "3-5|6-8|9-12") String ageGroup,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"short", "medium", "long"})
            @Pattern(regexp = "short|medium|long") String length,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"uk", "en"})
            @Pattern(regexp = "uk|en") String language
) {}
