package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.device.PushNotifier;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnabledIfEnvironmentVariable(named = "HUGGINGFACE_API_TOKEN", matches = ".+")
class StoryPushIntegrationIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder encoder;
    @MockitoBean PushNotifier pushNotifier;

    User me;
    String bearer;

    @BeforeEach
    void clean() {
        stories.deleteAll();
        users.deleteAll();
        me = seedUser("push-it@example.com");
        bearer = loginBearer("push-it@example.com");
    }

    @Test
    void should_invokePushNotifier_when_storyGenerationCompletes() {
        client().post().uri("/api/stories/generate")
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "theme", "test theme",
                        "characters", java.util.List.of("Hero"),
                        "ageGroup", "3-5",
                        "length", "short",
                        "language", "uk"))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockLast(java.time.Duration.ofMinutes(2));

        // Push fires asynchronously inside doOnNext — Awaitility handles the wait.
        await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() ->
                verify(pushNotifier, times(1)).notifyStoryReady(
                        eq(me.getId()),
                        anyString(),
                        anyString()));
    }

    private User seedUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("password123"));
        u.setDisplayName("Tester");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
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
