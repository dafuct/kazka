package com.kazka.child.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record UpdateCharacterRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "boy|girl|animal|creature|object") String kind,
        @NotBlank @Size(max = 280) String description,
        @Size(max = 8) List<@Size(max = 40) String> traits
) {}
