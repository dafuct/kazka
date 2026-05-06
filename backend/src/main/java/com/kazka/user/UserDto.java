package com.kazka.user;

public record UserDto(
        String id,
        String email,
        String displayName,
        UserRole role,
        boolean emailVerified,
        boolean googleLinked
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getRole(),
                u.isEmailVerified(),
                u.getGoogleSubject() != null
        );
    }
}
