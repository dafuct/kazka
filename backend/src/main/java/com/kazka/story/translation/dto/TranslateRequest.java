package com.kazka.story.translation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TranslateRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Pattern(regexp = "uk|en") String targetLanguage
) {}
