package com.kazka.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("kazka.auth")
public record AuthProperties(
        String appBaseUrl,
        String mailFrom,
        TokenTtl tokenTtl,
        Admin admin
) {
    public record TokenTtl(Duration emailVerification, Duration passwordReset) {}
    public record Admin(String email, String password) {}
}
