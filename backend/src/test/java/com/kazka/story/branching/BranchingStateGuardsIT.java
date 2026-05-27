package com.kazka.story.branching;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.hf.HuggingFaceClient;
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
    @MockitoBean HuggingFaceClient hfClient;

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
        Story s = seedBranchingStory(userA, profileA, "complete", null);
        authedClient(userA).post().uri("/api/stories/" + s.getId() + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("A"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void unknown_choice_id_returns_400() {
        Story s = seedBranchingStory(userA, profileA, "awaiting_choice_1",
                List.of(new BranchingChoice("A", "Option A"), new BranchingChoice("B", "Option B")));
        authedClient(userA).post().uri("/api/stories/" + s.getId() + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("Z"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void choose_on_other_users_story_returns_404() {
        Story s = seedBranchingStory(userA, profileA, "awaiting_choice_1",
                List.of(new BranchingChoice("A", "Option A"), new BranchingChoice("B", "Option B")));
        authedClient(userB).post().uri("/api/stories/" + s.getId() + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("A"))
                .exchange()
                .expectStatus().isNotFound();
    }

    private Story seedBranchingStory(String userId, String profileId, String state, List<BranchingChoice> choices) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId);
        s.setChildProfileId(profileId);
        s.setTitle("t");
        s.setTheme("th");
        s.setCharacters(List.of("c"));
        s.setAgeGroup("6-8");
        s.setLength("short");
        s.setLanguage("uk");
        s.setContent("body");
        s.setBranching(true);
        s.setBranchingState(state);
        s.setPendingChoices(choices);
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

    private String seedProfile(String userId) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId);
        p.setName("Лія");
        p.setAvatarSeed("s");
        p.setPreferredLanguage("uk");
        return profiles.save(p).getId();
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
