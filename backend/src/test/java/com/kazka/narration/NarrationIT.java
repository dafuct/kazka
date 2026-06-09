package com.kazka.narration;

import com.kazka.AbstractIT;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("integration")
class NarrationIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean GeminiTtsClient ttsClient;

    @BeforeEach
    void clean() {
        stories.deleteAll();
        users.deleteAll();
        when(ttsClient.synthesizePcm(any(), any())).thenReturn(Mono.just(new byte[]{1, 2, 3, 4}));
    }

    @Test
    void should_generateThenServeReadyUrl_when_ownerRequestsNarration() {
        User user = createVerifiedUser("o@example.com", "Opass123!");
        Story story = saveStory(user.getId());
        WebTestClient authed = authed("o@example.com", "Opass123!");

        authed.post().uri("/api/stories/" + story.getId() + "/narration")
                .exchange()
                .expectStatus().isAccepted()
                .expectBody().jsonPath("$.status").isEqualTo("GENERATING");

        await().atMost(ofSeconds(10)).untilAsserted(() ->
                authed.get().uri("/api/stories/" + story.getId() + "/narration")
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.status").isEqualTo("READY")
                        .jsonPath("$.url").value(url -> assertThat((String) url).contains(story.getId())));
    }

    @Test
    void should_allowNarration_when_adminRequestsOthersTale() {
        User user = createVerifiedUser("u@example.com", "Upass123!");
        createAdmin("admin@example.com", "Adminpass1!");
        Story story = saveStory(user.getId());
        WebTestClient adminClient = authed("admin@example.com", "Adminpass1!");

        adminClient.post().uri("/api/stories/" + story.getId() + "/narration")
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void should_return404_when_strangerRequestsNarration() {
        User owner = createVerifiedUser("owner@example.com", "Opass123!");
        createVerifiedUser("stranger@example.com", "Spass123!");
        Story story = saveStory(owner.getId());
        WebTestClient stranger = authed("stranger@example.com", "Spass123!");

        stranger.post().uri("/api/stories/" + story.getId() + "/narration")
                .exchange()
                .expectStatus().isNotFound();
    }

    private User createVerifiedUser(String email, String password) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setDisplayName(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }

    private User createAdmin(String email, String password) {
        User user = createVerifiedUser(email, password);
        user.setRole(UserRole.ADMIN);
        return users.save(user);
    }

    private Story saveStory(String userId) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(userId);
        story.setTitle("Лисичка");
        story.setTheme("forest");
        story.setCharacters(List.of("лисичка"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("Жила собі лисичка у темному лісі.");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        return stories.save(story);
    }

    /**
     * Build a WebTestClient carrying a Bearer token and the XSRF double-submit token.
     * Mirrors the pattern in StoryChildFilterIT / BranchingControllerIT:
     *   1. POST /api/auth/token/login → accessToken
     *   2. GET  /api/public/showcase  → XSRF-TOKEN cookie
     */
    private WebTestClient authed(String email, String password) {
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        String bearer = body.get("accessToken").toString();

        EntityExchangeResult<byte[]> csrf = client()
                .get().uri("/api/public/showcase")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult();
        MultiValueMap<String, ResponseCookie> csrfCookies = csrf.getResponseCookies();
        ResponseCookie xsrfCookie = csrfCookies.getFirst("XSRF-TOKEN");
        if (xsrfCookie == null) {
            throw new IllegalStateException("No XSRF-TOKEN cookie issued by server");
        }
        String csrfToken = xsrfCookie.getValue();

        return client().mutate()
                .defaultHeader("Authorization", "Bearer " + bearer)
                .defaultHeader("X-XSRF-TOKEN", csrfToken)
                .defaultCookie("XSRF-TOKEN", csrfToken)
                .responseTimeout(ofSeconds(20))
                .build();
    }
}
