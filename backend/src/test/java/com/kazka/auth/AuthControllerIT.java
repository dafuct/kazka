package com.kazka.auth;

import com.kazka.AbstractIT;
import com.kazka.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIT {

    @Autowired UserRepository users;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void should_returnUserAndSessionCookie_when_signup() {
        var result = client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", "alice@example.com",
                        "password", "password123",
                        "displayName", "Alice"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.user.email").isEqualTo("alice@example.com")
                .returnResult();

        List<String> cookies = result.getResponseHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(cookie -> cookie.startsWith("SESSION="));
    }

    @Test
    void should_returnTokensAndUser_when_signup() {
        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", "newsignup@example.com",
                        "password", "password123",
                        "displayName", "New Signup"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.user.email").isEqualTo("newsignup@example.com")
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.accessExpiresInSeconds").isNumber();
    }

    @Test
    void should_returnUser_when_callMeWithValidSession() {
        var sessionCookie = signupAndGetSessionCookie("bob@example.com", "Bob");

        client().get().uri("/api/auth/me")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("bob@example.com");
    }

    @Test
    void should_return401_when_callMeWithoutSession() {
        client().get().uri("/api/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void should_return409_when_signupEmailExists() {
        signupAndGetSessionCookie("dup@example.com", "Dup");

        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", "dup@example.com",
                        "password", "password123",
                        "displayName", "Dup2"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error").isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void should_includeSuspendedFalse_when_meCalledForActiveUser() {
        signupAndVerify("active@example.com");
        client().get().uri("/api/auth/me")
                .cookie("SESSION", login("active@example.com"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.suspended").isEqualTo(false);
    }

    @Test
    void should_includeSuspendedTrue_when_meCalledForSuspendedUser() {
        signupAndVerify("blocked@example.com");
        var user = users.findByEmail("blocked@example.com").orElseThrow();
        user.setSuspendedAt(java.time.Instant.now());
        user.setSuspendedReason("CONTENT_POLICY");
        users.save(user);
        client().get().uri("/api/auth/me")
                .cookie("SESSION", login("blocked@example.com"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.suspended").isEqualTo(true);
    }

    private String signupAndGetSessionCookie(String email, String displayName) {
        var result = client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", email, "password", "password123", "displayName", displayName))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(Void.class);
        ResponseCookie session = result.getResponseCookies().getFirst("SESSION");
        assertThat(session).isNotNull();
        return "SESSION=" + session.getValue();
    }

    private void signupAndVerify(String email) {
        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123", "displayName", "Tester"))
                .exchange().expectStatus().isCreated();
        var user = users.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        users.save(user);
    }

    private String login(String email) {
        var result = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class);
        ResponseCookie session = result.getResponseCookies().getFirst("SESSION");
        assertThat(session).isNotNull();
        return session.getValue();
    }
}
