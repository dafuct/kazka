package com.kazka.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutSessionRequest(
        @NotBlank String planId,
        @NotBlank String provider,
        String countryHint
) {}
