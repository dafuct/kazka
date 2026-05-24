package com.kazka.child.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateChildProfileRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 80) String name,
        @Min(1900) @Max(2100) Short birthYear,
        @Pattern(regexp = "boy|girl|other") String gender,
        @Schema(allowableValues = {"uk","en","bilingual"}) @Pattern(regexp = "uk|en|bilingual") String preferredLanguage,
        @Size(max = 10) List<@Size(max = 40) String> interests
) {}
