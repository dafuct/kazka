package com.kazka.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
        Paddle paddle,
        LiqPay liqpay,
        Monobank monobank,
        String successUrl,
        String cancelUrl
) {
    public record Paddle(String apiKey, String webhookSecret, String environment) {}
    public record LiqPay(String publicKey, String privateKey) {}
    public record Monobank(String token, String webhookPublicKey) {}
}
