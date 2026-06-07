package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
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
class StoryChildFilterIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @MockitoBean EntitlementResolver entitlements;

    String userId;
    String profileA;
    String profileB;
    String storyForA;
    String storyForB;
    String legacyStory; // no child_profile_id

    @BeforeEach
    void seed() {
        stories.deleteAll();
        profiles.deleteAll();
        users.deleteAll();

        when(entitlements.isPro(any())).thenReturn(true);
        userId = createUser();
        profileA = createProfile(userId, "A");
        profileB = createProfile(userId, "B");
        storyForA = createStory(userId, profileA);
        storyForB = createStory(userId, profileB);
        legacyStory = createStory(userId, null);
    }

    @Test
    void should_filter_stories_by_childProfileId() {
        var client = authedClient(userId);
        client.get().uri("/api/stories?childProfileId=" + profileA).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].id").isEqualTo(storyForA);
    }

    @Test
    void should_return_unattributed_stories_when_filter_is_none() {
        var client = authedClient(userId);
        client.get().uri("/api/stories?childProfileId=none").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].id").isEqualTo(legacyStory);
    }

    @Test
    void should_return_all_when_no_filter() {
        var client = authedClient(userId);
        client.get().uri("/api/stories").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(3);
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

    private String createProfile(String ownerId, String name) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(ownerId);
        profile.setName(name);
        profile.setPreferredLanguage("uk");
        profile.setInterests(List.of());
        profile.setAvatarSeed("seed-" + name.toLowerCase());
        profiles.save(profile);
        return profile.getId();
    }

    private String createStory(String ownerId, String childProfileId) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(ownerId);
        story.setTitle("Story for " + (childProfileId == null ? "none" : childProfileId));
        story.setTheme("theme");
        story.setCharacters(List.of("hero"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("content");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        story.setChildProfileId(childProfileId);
        stories.save(story);
        return story.getId();
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
