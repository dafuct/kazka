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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("integration")
class BranchingControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AiClient aiClient;

    String userId;
    String profileId;

    @BeforeEach
    void setup() {
        userId = seedUser();
        profileId = seedProfile(userId);
    }

    @Test
    void full_three_call_flow_persists_complete_tale() {
        // Three canned responses for the three LLM calls (in order).
        when(aiClient.streamText(anyString(), anyString())).thenReturn(
                Flux.just("Opening text.\n\n---\n\nCHOICE_A: Go left\nCHOICE_B: Go right"),
                Flux.just("Middle text.\n\n---\n\nCHOICE_A: Climb the tree\nCHOICE_B: Cross the bridge"),
                Flux.just("Closing text. The tale ends happily."));

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
        assertThat(saved.getContent()).contains("Opening text.");
        assertThat(saved.getContent()).contains("Go left");
        assertThat(saved.getContent()).contains("Middle text.");
        assertThat(saved.getContent()).contains("Cross the bridge");
        assertThat(saved.getContent()).contains("Closing text. The tale ends happily.");
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
