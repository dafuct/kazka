package com.kazka.story.translation;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.ai.AiClient;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.translation.dto.TranslateRequest;
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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
class TranslationControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EntitlementResolver entitlements;
    @MockitoBean AiClient aiClient;

    String userId;

    @BeforeEach
    void setup() {
        userId = seedUser();
        when(aiClient.streamText(anyString(), anyString()))
                .thenReturn(Flux.just("Once upon a time, a dragon lived."));
    }

    @Test
    void paid_user_translates_uk_to_en_and_persists() {
        when(entitlements.isPro(userId)).thenReturn(true);
        Story s = seedStory(userId, "uk", "Жив-був дракон.");

        authedClient(userId).post().uri("/api/stories/" + s.getId() + "/translate")
                .bodyValue(new TranslateRequest("en"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.translatedLanguage").isEqualTo("en")
                .jsonPath("$.translatedContent").isEqualTo("Once upon a time, a dragon lived.");

        var saved = stories.findById(s.getId()).orElseThrow();
        assertThat(saved.getTranslatedLanguage()).isEqualTo("en");
        assertThat(saved.getTranslatedContent()).isEqualTo("Once upon a time, a dragon lived.");
    }

    @Test
    void free_user_gets_402() {
        when(entitlements.isPro(userId)).thenReturn(false);
        Story s = seedStory(userId, "uk", "Жив-був дракон.");

        authedClient(userId).post().uri("/api/stories/" + s.getId() + "/translate")
                .bodyValue(new TranslateRequest("en"))
                .exchange()
                .expectStatus().isEqualTo(402);
    }

    @Test
    void same_language_gets_400() {
        when(entitlements.isPro(userId)).thenReturn(true);
        Story s = seedStory(userId, "uk", "Жив-був дракон.");

        authedClient(userId).post().uri("/api/stories/" + s.getId() + "/translate")
                .bodyValue(new TranslateRequest("uk"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void other_users_story_gets_404() {
        when(entitlements.isPro(userId)).thenReturn(true);
        String otherUser = seedUser();
        Story s = seedStory(otherUser, "uk", "Жив-був дракон.");

        authedClient(userId).post().uri("/api/stories/" + s.getId() + "/translate")
                .bodyValue(new TranslateRequest("en"))
                .exchange()
                .expectStatus().isNotFound();
    }

    private Story seedStory(String ownerId, String language, String content) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(ownerId); s.setTitle("t"); s.setTheme("th");
        s.setCharacters(List.of("дракон"));
        s.setAgeGroup("6-8"); s.setLength("short"); s.setLanguage(language);
        s.setContent(content);
        return stories.save(s);
    }

    private String seedUser() {
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
        if (xsrfCookie == null) throw new IllegalStateException("No XSRF-TOKEN cookie");
        String csrfToken = xsrfCookie.getValue();

        return client().mutate()
                .defaultHeader("Authorization", "Bearer " + bearer)
                .defaultHeader("X-XSRF-TOKEN", csrfToken)
                .defaultCookie("XSRF-TOKEN", csrfToken)
                .build();
    }
}
