package com.kazka.dashboard;

import com.kazka.AbstractIT;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
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
class DashboardControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void returns_dashboard_for_authed_user() {
        String userId = seedUser();
        String profileId = seedProfile(userId, "Лія");
        seedStory(userId, profileId, "Зачарований дракон");

        authedClient(userId).get().uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.aggregates.talesTotal").isEqualTo(1)
                .jsonPath("$.children.length()").isEqualTo(1)
                .jsonPath("$.children[0].name").isEqualTo("Лія")
                .jsonPath("$.children[0].taleCount").isEqualTo(1)
                .jsonPath("$.children[0].latestTale.title").isEqualTo("Зачарований дракон")
                .jsonPath("$.recentTales.length()").isEqualTo(1);
    }

    @Test
    void cross_user_data_does_not_leak() {
        String userA = seedUser();
        String userB = seedUser();
        String profileA = seedProfile(userA, "A-child");
        seedStory(userA, profileA, "A-tale");
        String profileB = seedProfile(userB, "B-child");
        seedStory(userB, profileB, "B-tale");

        authedClient(userA).get().uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.aggregates.talesTotal").isEqualTo(1)
                .jsonPath("$.children.length()").isEqualTo(1)
                .jsonPath("$.children[0].name").isEqualTo("A-child")
                .jsonPath("$.recentTales[0].title").isEqualTo("A-tale");
    }

    @Test
    void empty_user_returns_zero_state() {
        String userId = seedUser();
        authedClient(userId).get().uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.aggregates.talesTotal").isEqualTo(0)
                .jsonPath("$.children.length()").isEqualTo(0)
                .jsonPath("$.recentTales.length()").isEqualTo(0);
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

    private String seedProfile(String userId, String name) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId); profile.setName(name); profile.setAvatarSeed("s"); profile.setPreferredLanguage("uk");
        return profiles.save(profile).getId();
    }

    private Story seedStory(String userId, String profileId, String title) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(userId); story.setChildProfileId(profileId);
        story.setTitle(title); story.setTheme("th"); story.setCharacters(List.of("c"));
        story.setAgeGroup("6-8"); story.setLength("short"); story.setLanguage("uk");
        story.setContent("body");
        return stories.save(story);
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
        if (xsrfCookie == null) throw new IllegalStateException("No XSRF-TOKEN cookie");
        String csrfToken = xsrfCookie.getValue();

        return client().mutate()
                .defaultHeader("Authorization", "Bearer " + bearer)
                .defaultHeader("X-XSRF-TOKEN", csrfToken)
                .defaultCookie("XSRF-TOKEN", csrfToken)
                .build();
    }
}
