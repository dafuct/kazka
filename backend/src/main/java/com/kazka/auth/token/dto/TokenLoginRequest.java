package com.kazka.auth.token.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenLoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {}
