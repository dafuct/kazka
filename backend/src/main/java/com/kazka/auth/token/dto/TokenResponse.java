package com.kazka.auth.token.dto;

import com.kazka.user.UserDto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds,
        UserDto user
) {}
