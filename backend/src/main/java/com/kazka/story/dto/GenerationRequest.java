package com.kazka.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record GenerationRequest(
        @NotBlank String theme,
        @NotEmpty List<String> characters,
        @Pattern(regexp = "3-5|6-8|9-12") String ageGroup,
        @Pattern(regexp = "short|medium|long") String length,
        @Pattern(regexp = "uk|en") String language
) {}
