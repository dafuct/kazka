package com.kazka.user;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String displayName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserRole role,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean emailVerified,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean googleLinked,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean suspended
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isEmailVerified(),
                user.getGoogleSubject() != null,
                user.isSuspended());
    }
}
