package com.kazka.auth.token.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenLogoutRequest(@NotBlank String refreshToken) {}
