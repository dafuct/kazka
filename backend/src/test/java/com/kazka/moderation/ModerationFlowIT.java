package com.kazka.moderation;

import com.kazka.AbstractIT;
import com.kazka.user.UserRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("integration")
@Import(ModerationFlowIT.MockConfig.class)
class ModerationFlowIT extends AbstractIT {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        ModerationJudgeClient mockGuard() { return mock(ModerationJudgeClient.class); }
    }

    @Autowired UserRepository users;
    @Autowired FlaggedAttemptRepository flags;
    @Autowired ModerationJudgeClient guard;

    @BeforeEach
    void clean() throws Exception {
        flags.deleteAll();
        users.deleteAll();
        greenMail.purgeEmailFromAllMailboxes();
        clearInvocations(guard);
    }

    @Test
    void should_returnBlockedInputSseError_when_judgeRefusesPrompt() {
        signupAndVerify("kid@example.com");
        when(guard.classify(anyString(), anyString(), any()))
                .thenReturn(ModerationResult.Refused.of(ModerationCategory.SEXUAL));

        Session session = login("kid@example.com");
        var raw = client().post().uri("/api/stories/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .cookie("SESSION", session.sessionId())
                .header("X-XSRF-TOKEN", session.csrfToken())
                .cookie("XSRF-TOKEN", session.csrfToken())
                .bodyValue(new HashMap<String, Object>() {{
                    put("theme", "naked princess");
                    put("characters", List.of("Sofia"));
                    put("ageGroup", "6-8");
                    put("length", "short");
                    put("language", "uk");
                    put("childProfileId", "profile-123");
                    put("includeCharacterIds", null);
                }})
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(raw).contains("event:error").contains("\"code\":\"BLOCKED_INPUT\"");
        assertThat(flags.findAll()).hasSize(1);
    }

    @Test
    void should_suspendAccount_when_thirdRefusalArrivesIn24h() {
        signupAndVerify("repeat@example.com");
        when(guard.classify(anyString(), anyString(), any()))
                .thenReturn(ModerationResult.Refused.of(ModerationCategory.SEXUAL));

        Session session = login("repeat@example.com");
        for (int index = 0; index < 3; index++) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("theme", "bad");
            requestBody.put("characters", List.of("x"));
            requestBody.put("ageGroup", "6-8");
            requestBody.put("length", "short");
            requestBody.put("language", "uk");
            requestBody.put("childProfileId", "profile-test");
            requestBody.put("includeCharacterIds", null);

            client().post().uri("/api/stories/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .cookie("SESSION", session.sessionId())
                    .header("X-XSRF-TOKEN", session.csrfToken())
                    .cookie("XSRF-TOKEN", session.csrfToken())
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk();
        }

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var user = users.findByEmail("repeat@example.com").orElseThrow();
            assertThat(user.isSuspended()).isTrue();
            assertThat(user.getSuspendedReason()).isEqualTo("CONTENT_POLICY");
            assertThat(greenMail.getReceivedMessages().length).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void should_return403_when_suspendedUserAttemptsGenerate() {
        signupAndVerify("blocked@example.com");
        var user = users.findByEmail("blocked@example.com").orElseThrow();
        user.setSuspendedAt(java.time.Instant.now());
        user.setSuspendedReason("CONTENT_POLICY");
        users.save(user);

        Session session = login("blocked@example.com");
        var body = client().post().uri("/api/stories/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .cookie("SESSION", session.sessionId())
                .header("X-XSRF-TOKEN", session.csrfToken())
                .cookie("XSRF-TOKEN", session.csrfToken())
                .bodyValue(new HashMap<String, Object>() {{
                    put("theme", "anything");
                    put("characters", List.of("x"));
                    put("ageGroup", "6-8");
                    put("length", "short");
                    put("language", "uk");
                    put("childProfileId", "profile-123");
                    put("includeCharacterIds", null);
                }})
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        // Spring WebFlux wraps the error handler response in SSE format (data: prefix)
        // because the endpoint produces text/event-stream; the JSON payload is correct.
        assertThat(body).contains("ACCOUNT_SUSPENDED");
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

    /**
     * Returns a session with both the SESSION cookie and the XSRF-TOKEN cookie.
     * The CSRF token is obtained by making a GET to /api/auth/me after login,
     * since the login endpoint is excluded from CSRF and does not trigger the
     * CSRF cookie to be set.
     */
    private Session login(String email) {
        var loginResult = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class);

        ResponseCookie sessionCookie = loginResult.getResponseCookies().getFirst("SESSION");
        String sessionId = sessionCookie != null ? sessionCookie.getValue() : "";

        // The XSRF-TOKEN cookie is not set on the login response (login is CSRF-excluded).
        // Fetch it by making an authenticated GET which triggers the CSRF filter.
        var meResult = client().get().uri("/api/auth/me")
                .cookie("SESSION", sessionId)
                .exchange()
                .returnResult(String.class);

        MultiValueMap<String, ResponseCookie> meCookies = meResult.getResponseCookies();
        ResponseCookie csrfCookie = meCookies.getFirst("XSRF-TOKEN");
        String csrfToken = csrfCookie != null ? csrfCookie.getValue() : "";

        return new Session(sessionId, csrfToken);
    }

    record Session(String sessionId, String csrfToken) {}
}
