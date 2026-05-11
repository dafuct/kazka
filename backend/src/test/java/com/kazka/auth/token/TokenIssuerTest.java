package com.kazka.auth.token;

import com.kazka.auth.AuthProperties;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenIssuerTest {

    TokenIssuer issuer;

    @BeforeEach
    void setUp() {
        var jwt = new AuthProperties.Jwt(
                "test-secret-32-characters-minimum-for-hs256-signing-ok",
                Duration.ofMinutes(5),
                Duration.ofHours(1),
                "kazka-test");
        issuer = new TokenIssuer(jwt);
    }

    @Test
    void should_roundTripUserIdAndRole_when_validTokenIssued() {
        String token = issuer.issueAccessToken("user-123", UserRole.USER);

        TokenIssuer.Claims claims = issuer.verifyAccessToken(token);

        assertThat(claims.userId()).isEqualTo("user-123");
        assertThat(claims.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void should_includeIssuer_when_tokenIssued() {
        String token = issuer.issueAccessToken("u", UserRole.ADMIN);
        TokenIssuer.Claims claims = issuer.verifyAccessToken(token);
        assertThat(claims.issuer()).isEqualTo("kazka-test");
    }

    @Test
    void should_throw_when_tokenSignatureInvalid() {
        String token = issuer.issueAccessToken("u", UserRole.USER);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThatThrownBy(() -> issuer.verifyAccessToken(tampered))
                .isInstanceOf(TokenIssuer.InvalidTokenException.class);
    }

    @Test
    void should_throw_when_tokenExpired() {
        var shortJwt = new AuthProperties.Jwt(
                "test-secret-32-characters-minimum-for-hs256-signing-ok",
                Duration.ofMillis(-1),
                Duration.ofHours(1),
                "kazka-test");
        var shortLived = new TokenIssuer(shortJwt);
        String token = shortLived.issueAccessToken("u", UserRole.USER);

        assertThatThrownBy(() -> shortLived.verifyAccessToken(token))
                .isInstanceOf(TokenIssuer.InvalidTokenException.class);
    }
}
