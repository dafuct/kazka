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
        assertThat(cookies).anyMatch(c -> c.startsWith("SESSION="));
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
}
