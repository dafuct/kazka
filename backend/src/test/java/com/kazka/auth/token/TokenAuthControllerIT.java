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
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setDisplayName("Test");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return users.save(u);
    }

    @Test
    void should_returnAccessAndRefreshTokens_when_loginWithValidCredentials() {
        User u = seedUser("alice@example.com", "password123");

        var body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "alice@example.com", "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.user.id").isEqualTo(u.getId())
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
        User u = seedUser("carol@example.com", "password123");

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
}
