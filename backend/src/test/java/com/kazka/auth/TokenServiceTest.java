package com.kazka.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @Test
    void should_returnUrlSafeToken_when_generate() {
        String token = tokenService.generate();

        assertThat(token).hasSize(43);
        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void should_returnDifferentTokens_when_generateCalledTwice() {
        assertThat(tokenService.generate()).isNotEqualTo(tokenService.generate());
    }
}
