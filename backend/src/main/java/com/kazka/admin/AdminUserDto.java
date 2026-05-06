package com.kazka.admin;

import com.kazka.user.UserRole;

import java.time.Instant;

public record AdminUserDto(
        String id,
        String email,
        String displayName,
        UserRole role,
        boolean emailVerified,
        boolean googleLinked,
        Instant createdAt,
        long storyCount
) {}
