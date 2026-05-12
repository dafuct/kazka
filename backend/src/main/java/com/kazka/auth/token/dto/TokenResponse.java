package com.kazka.auth.token.dto;

import com.kazka.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String refreshToken,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long accessExpiresInSeconds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserDto user
) {}
