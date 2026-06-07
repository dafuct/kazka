package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.ai.AiClient;
import com.kazka.moderation.ModerationResult;
import com.kazka.moderation.ModerationService;
import com.kazka.story.dto.GenerationRequest;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("integration")
@Import(StoryGenerationWithChildIT.MockConfig.class)
class StoryGenerationWithChildIT extends AbstractIT {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        AiClient mockAi() {
            AiClient aiClient = mock(AiClient.class);
            when(aiClient.streamText(anyString(), anyString()))
                    .thenReturn(Flux.just("Пригода\n\nЖив-був дракон."));
            when(aiClient.streamEdit(anyString(), anyString()))
                    .thenReturn(Flux.just("Пригода\n\nЖив-був дракон."));
            return aiClient;
        }

        @Bean @Primary
        ModerationService mockModeration() {
            ModerationService moderation = mock(ModerationService.class);
            when(moderation.checkInput(anyString(), anyString(), any()))
                    .thenReturn(ModerationResult.Allowed.INSTANCE);
            return moderation;
        }
    }

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @Autowired com.kazka.child.StoryCharacterRepository storyCharacters;

    String userId;
    String profileId;

    @AfterEach
    void tearDown() {
        // clean up in FK-safe order after each test so other test classes
        // that call users.deleteAll() don't hit FK constraint violations
        storyCharacters.deleteAll();
        stories.deleteAll();
        profiles.deleteAll();
        users.deleteAll();
    }

    @BeforeEach
    void seed() {
        // also clean up before each test for isolation
        storyCharacters.deleteAll();
        stories.deleteAll();
        profiles.deleteAll();
        users.deleteAll();


        userId = createUser();
        profileId = createProfile(userId);
    }

    @Test
    void should_reject_when_childProfileId_missing() {
        WebTestClient cli = authedClient(userId);
        cli.post().uri("/api/stories/generate")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "theme", "пригода", "characters", List.of("дракон"),
                        "ageGroup", "6-8", "length", "short", "language", "uk"))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void should_persist_childProfileId_on_generated_story() {
        WebTestClient cli = authedClient(userId);
        cli.post().uri("/api/stories/generate")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GenerationRequest("пригода", List.of("дракон"), "6-8", "short", "uk",
                        profileId, List.of()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class).getResponseBody().blockLast();

        assertThat(stories.findAllByUserIdOrderByCreatedAtDesc(userId,
                        org.springframework.data.domain.PageRequest.of(0, 5))
                .getContent().get(0).getChildProfileId()).isEqualTo(profileId);
    }

    private String createUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.example");
        user.setDisplayName("Tester");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);
        return id;
    }

    private String createProfile(String userId) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId);
        profile.setName("Тест");
        profile.setPreferredLanguage("uk");
        profile.setInterests(List.of());
        profile.setAvatarSeed("abc123");
        profiles.save(profile);
        return profile.getId();
    }

    private WebTestClient authedClient(String userId) {
        String email = users.findById(userId).orElseThrow().getEmail();
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
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
                .build();
    }
}
