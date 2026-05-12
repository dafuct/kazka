package com.kazka.auth;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCoexistenceIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void should_serveBothCookieAndBearerOnMe_when_sameBackendHandlesBothModes() {
        seedUser("cookie@example.com");
        seedUser("bearer@example.com");

        // Web flow: login via /api/auth/login → cookie
        ResponseCookie cookieSession = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "cookie@example.com", "password", "password123"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(Void.class)
                .getResponseCookies().getFirst("SESSION");
        assertThat(cookieSession).isNotNull();

        // Mobile flow: login via /api/auth/token/login → bearer JWT
        @SuppressWarnings("rawtypes")
        Map tokenBody = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "bearer@example.com", "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class)
                .getResponseBody().blockFirst();
        String bearerToken = tokenBody.get("accessToken").toString();

        // Cookie request: resolves to cookie@example.com
        client().get().uri("/api/auth/me")
                .cookie("SESSION", cookieSession.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("cookie@example.com");

        // Bearer request: resolves to bearer@example.com
        client().get().uri("/api/auth/me")
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("bearer@example.com");

        // Alternate: cookie again, then bearer again — both still resolve correctly
        client().get().uri("/api/auth/me")
                .cookie("SESSION", cookieSession.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("cookie@example.com");

        client().get().uri("/api/auth/me")
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("bearer@example.com");
    }

    @Test
    void should_treatBearerAsHigherPriority_when_bothHeadersPresent() {
        seedUser("cookie@example.com");
        seedUser("bearer@example.com");

        ResponseCookie cookieSession = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "cookie@example.com", "password", "password123"))
                .exchange().returnResult(Void.class)
                .getResponseCookies().getFirst("SESSION");

        @SuppressWarnings("rawtypes")
        Map tokenBody = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "bearer@example.com", "password", "password123"))
                .exchange().returnResult(Map.class).getResponseBody().blockFirst();
        String bearerToken = tokenBody.get("accessToken").toString();

        // Both cookie AND bearer in the same request: bearer wins.
        // Filter order in SecurityConfig: loginFilter then bearerFilter are both
        // registered at SecurityWebFiltersOrder.AUTHENTICATION. loginFilter only
        // matches POST /api/auth/login so it's a no-op here. bearerFilter runs and
        // sets the authentication from the Authorization header, overriding any
        // cookie-session SecurityContext that may already be present.
        client().get().uri("/api/auth/me")
                .cookie("SESSION", cookieSession.getValue())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("bearer@example.com");
    }

    private void seedUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("password123"));
        u.setDisplayName("Test");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
    }
}
