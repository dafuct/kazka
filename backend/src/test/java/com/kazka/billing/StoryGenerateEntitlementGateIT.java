package com.kazka.billing;

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
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class StoryGenerateEntitlementGateIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void should_return402_when_freeUserHitsMonthlyLimit() {
        seedUserAtLimit("gated-it@example.com");
        String bearer = loginBearer("gated-it@example.com");
        Csrf csrf = fetchCsrf();

        Map<String, Object> body = new HashMap<>();
        body.put("theme", "forest");
        body.put("characters", List.of("fox", "owl"));
        body.put("ageGroup", "3-5");
        body.put("length", "short");
        body.put("language", "uk");
        body.put("childProfileId", "profile-123");
        body.put("includeCharacterIds", null);

        client().post().uri("/api/stories/generate")
                .header("Authorization", "Bearer " + bearer)
                .header("X-XSRF-TOKEN", csrf.token)
                .cookie("XSRF-TOKEN", csrf.token)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(402)
                .expectBody(String.class)
                .consumeWith(r -> org.assertj.core.api.Assertions.assertThat(r.getResponseBody())
                        .as("error body must signal PAYWALL_REQUIRED")
                        .contains("PAYWALL_REQUIRED"));
    }

    /** Pull a fresh CSRF cookie value from any safe GET; CookieServerCsrfTokenRepository emits it. */
    private Csrf fetchCsrf() {
        EntityExchangeResult<byte[]> r = client().get().uri("/api/billing/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult();
        MultiValueMap<String, ResponseCookie> cookies = r.getResponseCookies();
        ResponseCookie c = cookies.getFirst("XSRF-TOKEN");
        if (c == null) {
            throw new IllegalStateException("No XSRF-TOKEN cookie issued by server");
        }
        return new Csrf(c.getValue());
    }

    private record Csrf(String token) {}

    private User seedUserAtLimit(String email) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("password123"));
        u.setDisplayName("Tester");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        u.setStoriesThisMonth(3); // at default free limit
        return users.save(u);
    }

    private String loginBearer(String email) {
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        return body.get("accessToken").toString();
    }
}
