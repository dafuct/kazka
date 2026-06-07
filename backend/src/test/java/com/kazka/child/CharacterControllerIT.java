package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.child.dto.CharacterDto;
import com.kazka.child.dto.ConfirmCharactersRequest;
import com.kazka.child.dto.ExtractedCandidateDto;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("integration")
class CharacterControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;

    String userId;
    String profileId;
    String storyId;

    @BeforeEach
    void seed() {
        userId = createUser();
        profileId = createProfile(userId);
        storyId = createStory(userId, profileId);
    }

    @Test
    void should_confirm_characters_then_list_them() {
        WebTestClient cli = authedClient(userId);
        var req = new ConfirmCharactersRequest(storyId, List.of(
                new ExtractedCandidateDto("Мурка", "animal", "a curious cat", List.of("curious"), "companion")));
        cli.post().uri("/api/characters/confirm?childProfileId=" + profileId)
                .bodyValue(req).exchange().expectStatus().isOk();

        cli.get().uri("/api/children/" + profileId + "/characters").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Мурка");
    }

    @Test
    void should_allow_freeTier_to_confirm_characters() {
        WebTestClient cli = authedClient(userId);
        var req = new ConfirmCharactersRequest(storyId, List.of(
                new ExtractedCandidateDto("Олег", "boy", "kind", List.of(), "protagonist")));
        cli.post().uri("/api/characters/confirm?childProfileId=" + profileId)
                .bodyValue(req).exchange().expectStatus().isOk();

        cli.get().uri("/api/children/" + profileId + "/characters").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Олег");
    }

    @Test
    void should_softArchive_on_delete() {
        WebTestClient cli = authedClient(userId);
        var saveReq = new ConfirmCharactersRequest(storyId, List.of(
                new ExtractedCandidateDto("Олег", "boy", "kind", List.of(), "protagonist")));
        CharacterDto[] saved = cli.post().uri("/api/characters/confirm?childProfileId=" + profileId)
                .bodyValue(saveReq).exchange().expectStatus().isOk()
                .returnResult(CharacterDto[].class).getResponseBody().blockFirst();
        String charId = saved[0].id();

        cli.delete().uri("/api/characters/" + charId).exchange().expectStatus().isNoContent();

        cli.get().uri("/api/children/" + profileId + "/characters").exchange()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private String createProfile(String ownerId) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(ownerId);
        profile.setName("TestChild");
        profile.setPreferredLanguage("uk");
        profile.setAvatarSeed("seed");
        profile.setInterests(List.of());
        profiles.save(profile);
        return profile.getId();
    }

    private String createStory(String ownerId, String childProfileId) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(ownerId);
        story.setChildProfileId(childProfileId);
        story.setTitle("Test story");
        story.setTheme("adventure");
        story.setCharacters(List.of());
        story.setAgeGroup("3-5");
        story.setLength("short");
        story.setContent("Once upon a time...");
        stories.save(story);
        return story.getId();
    }

    /**
     * Obtains a Bearer access token via /api/auth/token/login and a CSRF token from
     * a safe GET, then returns a WebTestClient pre-configured with both.
     * The CSRF cookie+header pair is required for mutating requests (POST/PATCH/DELETE).
     */
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

        // Fetch CSRF token from any safe GET endpoint
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
