package com.kazka.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("kazka.auth")
public record AuthProperties(
        String appBaseUrl,
        String mailFrom,
        TokenTtl tokenTtl,
        Admin admin,
        Jwt jwt,
        Apple apple
) {
    public record TokenTtl(Duration emailVerification, Duration passwordReset) {}

    public record Admin(String email, String password) {}

    public record Jwt(
            String secret,
            Duration accessTtl,
            Duration refreshTtl,
            String issuer
    ) {}

    public record Apple(
            String teamId,
            String clientId,
            String keyId,
            String privateKeyPem,
            String jwksUri,
            String issuer,
            Duration clientSecretTtl
    ) {}
}
