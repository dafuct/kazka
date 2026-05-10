package com.kazka.moderation;

import java.time.Instant;

public record SuspendedUserDto(
        String id,
        String email,
        String displayName,
        Instant suspendedAt,
        String suspendedReason,
        String suspendedBy
) {}
