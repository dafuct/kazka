package com.kazka.billing.dto;

public record CheckoutSessionResponse(
        String provider,
        String checkoutUrl,
        String providerReference
) {}
