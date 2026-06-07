package com.kazka.story.branching;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.ai.AiClient;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.branching.dto.BranchingChoice;
import com.kazka.story.branching.dto.BranchingChoiceRequest;
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
class BranchingStateGuardsIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EntitlementResolver entitlements;
    @MockitoBean AiClient aiClient;

    String userA;
    String userB;
    String profileA;

    @BeforeEach
    void setup() {
        when(entitlements.isPro(any())).thenReturn(true);
        userA = seedUser();
        userB = seedUser();
        profileA = seedProfile(userA);
    }

    @Test
    void choose_on_complete_story_returns_400() {
        Story story = seedBranchingStory(userA, profileA, "complete", null);
        authedClient(userA).post().uri("/api/stories/" + story.getId() + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("A"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void unknown_choice_id_returns_400() {
        Story storyForUnknownChoice = seedBranchingStory(userA, profileA, "awaiting_choice_1",
                List.of(new BranchingChoice("A", "Option A"), new BranchingChoice("B", "Option B")));
        authedClient(userA).post().uri("/api/stories/" + storyForUnknownChoice.getId() + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("Z"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void choose_on_other_users_story_returns_404() {
        Story storyForOtherUser = seedBranchingStory(userA, profileA, "awaiting_choice_1",
                List.of(new BranchingChoice("A", "Option A"), new BranchingChoice("B", "Option B")));
        authedClient(userB).post().uri("/api/stories/" + storyForOtherUser.getId() + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("A"))
                .exchange()
                .expectStatus().isNotFound();
    }

    private Story seedBranchingStory(String userId, String profileId, String state, List<BranchingChoice> choices) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(userId);
        story.setChildProfileId(profileId);
        story.setTitle("t");
        story.setTheme("th");
        story.setCharacters(List.of("c"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("body");
        story.setBranching(true);
        story.setBranchingState(state);
        story.setPendingChoices(choices);
        return stories.save(story);
    }

    private String seedUser() {
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

    private String seedProfile(String userId) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId);
        profile.setName("Лія");
        profile.setAvatarSeed("s");
        profile.setPreferredLanguage("uk");
        return profiles.save(profile).getId();
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
