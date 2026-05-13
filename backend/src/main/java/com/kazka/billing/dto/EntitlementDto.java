package com.kazka.billing.dto;

import com.kazka.billing.EntitlementState;
import com.kazka.billing.UserEntitlement;

import java.time.Instant;

public record EntitlementDto(
        String productAppleId,
        EntitlementState state,
        Instant expiresAt
) {
    public static EntitlementDto from(UserEntitlement e, String appleProductId) {
        return new EntitlementDto(appleProductId, e.getState(), e.getExpiresAt());
    }
}
