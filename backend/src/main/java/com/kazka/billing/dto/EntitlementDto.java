package com.kazka.billing.dto;

import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.UserEntitlement;

import java.time.Instant;

public record EntitlementDto(
        String productAppleId,
        String state,
        Instant expiresAt,
        String source
) {
    public static EntitlementDto from(UserEntitlement e, SubscriptionProduct p) {
        return new EntitlementDto(
                p.getAppleProductId(),
                e.getState().name(),
                e.getExpiresAt(),
                e.getSource().name()
        );
    }
}
