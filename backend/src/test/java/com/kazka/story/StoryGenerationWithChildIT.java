package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.hf.HuggingFaceClient;
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
        HuggingFaceClient mockHf() {
            HuggingFaceClient m = mock(HuggingFaceClient.class);
            when(m.streamText(anyString(), anyString()))
                    .thenReturn(Flux.just("Пригода\n\nЖив-був дракон."));
            when(m.streamEdit(anyString(), anyString()))
                    .thenReturn(Flux.just("Пригода\n\nЖив-був дракон."));
            return m;
        }

        @Bean @Primary
        ModerationService mockModeration() {
            ModerationService m = mock(ModerationService.class);
            when(m.checkInput(anyString(), anyString(), any()))
                    .thenReturn(ModerationResult.Allowed.INSTANCE);
            return m;
        }
    }

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @MockitoBean EntitlementResolver entitlements;
    @Autowired com.kazka.child.StoryCharacterRepository storyCharacters;
    @Autowired com.kazka.billing.UserEntitlementRepository entitlementRepo;

    String userId;
    String profileId;

    @AfterEach
    void tearDown() {
        // clean up in FK-safe order after each test so other test classes
        // that call users.deleteAll() don't hit FK constraint violations
        storyCharacters.deleteAll();
        stories.deleteAll();
        profiles.deleteAll();
        entitlementRepo.deleteAll();
        users.deleteAll();
    }

    @BeforeEach
    void seed() {
        // also clean up before each test for isolation
        storyCharacters.deleteAll();
        stories.deleteAll();
        profiles.deleteAll();
        entitlementRepo.deleteAll();
        users.deleteAll();

        when(entitlements.isPro(any())).thenReturn(true);

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
        User u = new User();
        u.setId(id);
        u.setEmail(id + "@test.example");
        u.setDisplayName("Tester");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
        return id;
    }

    private String createProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName("Тест");
        p.setPreferredLanguage("uk");
        p.setInterests(List.of());
        p.setAvatarSeed("abc123");
        profiles.save(p);
        return p.getId();
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
                .get().uri("/api/billing/products")
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
