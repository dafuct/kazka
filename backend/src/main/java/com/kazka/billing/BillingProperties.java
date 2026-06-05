package com.kazka.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("kazka.billing")
public record BillingProperties(
        String bundleId,
        String appAppleId,
        Long appleId,
        String environment,
        String issuerId,
        String keyId,
        String privateKeyPem,
        Boolean enabled,
        Integer freeMonthlyStoryLimit,
        PayPro paypro,
        Monobank monobank,
        String successUrl,
        String cancelUrl
) {
    public record PayPro(Integer vendorId, String apiKey, String ipnSecret, Boolean useTestMode) {}
    public record Monobank(String token, Recurring recurring) {
        public record Recurring(
                Duration tickInterval,
                Integer graceMaxRetries,
                String idempotencyPrefix
        ) {}
    }
}
