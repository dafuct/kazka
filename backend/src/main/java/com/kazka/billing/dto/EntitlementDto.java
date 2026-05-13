package com.kazka.billing.dto;

import com.kazka.billing.EntitlementState;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.UserEntitlement;

import java.time.Instant;

public record EntitlementDto(
        String productAppleId,
        EntitlementState state,
        Instant expiresAt
) {
    public static EntitlementDto from(UserEntitlement e, SubscriptionProduct p) {
        if (!p.getId().equals(e.getProductId())) {
            throw new IllegalArgumentException(
                    "Entitlement productId=" + e.getProductId() +
                    " does not match product " + p.getId());
        }
        return new EntitlementDto(p.getAppleProductId(), e.getState(), e.getExpiresAt());
    }
}
