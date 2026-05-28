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
    public static EntitlementDto from(UserEntitlement entitlement, SubscriptionProduct product) {
        return new EntitlementDto(
                product.getAppleProductId(),
                entitlement.getState().name(),
                entitlement.getExpiresAt(),
                entitlement.getSource().name()
        );
    }
}
