package com.kazka.auth.dto;

import com.kazka.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserDto user
) {}
