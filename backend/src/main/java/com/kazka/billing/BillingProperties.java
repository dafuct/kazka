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
        Integer freeMonthlyStoryLimit
) {}
