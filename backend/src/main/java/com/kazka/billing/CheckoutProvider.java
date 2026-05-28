package com.kazka.billing;

import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.user.User;
import reactor.core.publisher.Mono;

public interface CheckoutProvider {

    String provider();

    Mono<CheckoutSessionResponse> createSession(User user, SubscriptionProduct product);

    default String requireConfigured(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " not configured for this product");
        }
        return value;
    }
}
