package com.kazka.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 1, max = 100)
        @Pattern(regexp = "^[\\p{L}\\p{M}0-9 .'\\-]+$",
                 message = "must contain only letters, numbers, spaces, hyphens, apostrophes, or periods")
        String displayName
) {}
