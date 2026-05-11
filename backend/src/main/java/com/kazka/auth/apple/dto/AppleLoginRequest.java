package com.kazka.auth.apple.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
        @NotBlank String identityToken,
        String authorizationCode,
        String fullName,
        String email
) {}
