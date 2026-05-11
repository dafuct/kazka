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
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getRole(),
                u.isEmailVerified(),
                u.getGoogleSubject() != null,
                u.isSuspended());
    }
}
