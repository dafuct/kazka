package com.kazka.auth.token;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAuthControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired TokenIssuer issuer;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    private User seedUser(String email, String password) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setDisplayName("Test");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }

    @Test
    void should_returnAccessAndRefreshTokens_when_loginWithValidCredentials() {
        User seededUser = seedUser("alice@example.com", "password123");

        var body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "alice@example.com", "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.user.id").isEqualTo(seededUser.getId())
                .jsonPath("$.user.email").isEqualTo("alice@example.com")
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
    }

    @Test
    void should_return401_when_loginWithBadPassword() {
        seedUser("bob@example.com", "password123");

        client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "bob@example.com", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void should_return401_when_loginWithUnknownEmail() {
        client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "nobody@example.com", "password", "password123"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void should_acceptBearerToken_when_accessTokenUsedOnMeEndpoint() {
        User carolUser = seedUser("carol@example.com", "password123");

        @SuppressWarnings("rawtypes")
        Map loginBody = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "carol@example.com", "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class)
                .getResponseBody().blockFirst();
        String access = loginBody.get("accessToken").toString();

        client().get().uri("/api/auth/me")
                .header("Authorization", "Bearer " + access)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("carol@example.com");
    }

    @Test
    void should_issueNewTokens_when_refreshWithValidRefreshToken() {
        seedUser("dave@example.com", "password123");

        @SuppressWarnings("rawtypes")
        Map loginBody = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "dave@example.com", "password", "password123"))
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        String oldAccess = loginBody.get("accessToken").toString();
        String oldRefresh = loginBody.get("refreshToken").toString();

        @SuppressWarnings("rawtypes")
        Map refreshBody = client().post().uri("/api/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", oldRefresh))
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();

        String newAccess = refreshBody.get("accessToken").toString();
        String newRefresh = refreshBody.get("refreshToken").toString();
        assertThat(newAccess).isNotEqualTo(oldAccess);
        assertThat(newRefresh).isNotEqualTo(oldRefresh);

        // Old refresh token is now revoked
        client().post().uri("/api/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", oldRefresh))
                .exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void should_return401_when_refreshWithUnknownToken() {
        client().post().uri("/api/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", "not-a-real-token-just-padding-to-the-right-length"))
                .exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void should_revokeRefreshToken_when_logoutCalled() {
        seedUser("eve@example.com", "password123");

        @SuppressWarnings("rawtypes")
        Map loginBody = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "eve@example.com", "password", "password123"))
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        String refresh = loginBody.get("refreshToken").toString();

        client().post().uri("/api/auth/token/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", refresh))
                .exchange().expectStatus().isNoContent();

        // Subsequent refresh fails
        client().post().uri("/api/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", refresh))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void should_acceptUnknownRefreshToken_when_logoutCalled() {
        // Logout is idempotent — unknown tokens succeed silently
        client().post().uri("/api/auth/token/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", "unknown-token-padded-out-to-some-length"))
                .exchange().expectStatus().isNoContent();
    }
}
