package com.kazka.story.branching;

import com.kazka.AbstractIT;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.ai.AiClient;
import com.kazka.story.StoryRepository;
import com.kazka.story.branching.dto.BranchingChoiceRequest;
import com.kazka.story.branching.dto.BranchingStartRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("integration")
class BranchingControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AiClient aiClient;
    // Branching now triggers the comic build on completion; mock it so the IT stays hermetic
    // (no real image pipeline / Nano Banana HTTP call) and we can assert the trigger fires.
    @MockitoBean com.kazka.comics.ComicsBuilder comicsBuilder;

    String userId;
    String profileId;

    @BeforeEach
    void setup() {
        userId = seedUser();
        profileId = seedProfile(userId);
    }

    @Test
    void full_three_call_flow_persists_complete_tale() {
        // Three canned STRUCTURED-JSON responses for the three LLM calls (in order): the opening
        // carries its title in a field, continuations are pure "segment" prose — so no title,
        // label, or marker can leak into the tale, and it is never re-titled or restarted.
        when(aiClient.generateStoryJson(anyString(), anyString())).thenReturn(
                Mono.just("{\"title\":\"Місячний Сад\",\"segment\":\"Opening text.\",\"choiceA\":\"Go left\",\"choiceB\":\"Go right\"}"),
                Mono.just("{\"segment\":\"Middle text.\",\"choiceA\":\"Climb the tree\",\"choiceB\":\"Cross the bridge\"}"),
                Mono.just("{\"segment\":\"Closing text. The tale ends happily.\"}"));
        when(comicsBuilder.build(anyString())).thenReturn(Mono.empty());

        var cli = authedClient(userId);

        // Segment 1: start
        var r1 = cli.post().uri("/api/stories/branching")
                .bodyValue(new BranchingStartRequest("пригода", List.of("дракон"), "6-8", "short", "uk",
                        profileId, List.of()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.segmentNumber").isEqualTo(1)
                .jsonPath("$.branchingState").isEqualTo("awaiting_choice_1")
                .jsonPath("$.isFinal").isEqualTo(false)
                .jsonPath("$.choices.length()").isEqualTo(2)
                .jsonPath("$.content").isEqualTo("Opening text.")
                .returnResult();
        String storyId = new String(r1.getResponseBody())
                .replaceAll("(?s).*\"storyId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // Segment 2: choose A
        cli.post().uri("/api/stories/" + storyId + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("A"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.segmentNumber").isEqualTo(2)
                .jsonPath("$.branchingState").isEqualTo("awaiting_choice_2")
                .jsonPath("$.isFinal").isEqualTo(false)
                .jsonPath("$.choices.length()").isEqualTo(2);

        // Segment 3: choose B → final
        cli.post().uri("/api/stories/" + storyId + "/branching/choose")
                .bodyValue(new BranchingChoiceRequest("B"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.segmentNumber").isEqualTo(3)
                .jsonPath("$.branchingState").isEqualTo("complete")
                .jsonPath("$.isFinal").isEqualTo(true);

        var saved = stories.findById(storyId).orElseThrow();
        assertThat(saved.getBranchingState()).isEqualTo("complete");
        assertThat(saved.getPendingChoices()).isNull();
        // The title is lifted from the opening — NOT left inside the tale, NOT repeated per segment.
        assertThat(saved.getTitle()).isEqualTo("Місячний Сад");
        // The three narrative segments are stitched together EXACTLY once each — no tripling, no
        // title inside the body, no choice labels / CHOICE_ markers / "chose:" breadcrumb.
        assertThat(saved.getContent())
                .isEqualTo("Opening text.\n\nMiddle text.\n\nClosing text. The tale ends happily.")
                .doesNotContain("Місячний Сад", "Go left", "Cross the bridge", "CHOICE_", "обрал", "chose");
        // The finished interactive tale kicks off its comic cover build.
        verify(comicsBuilder).build(storyId);
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
