package com.kazka.dashboard;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
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
    @MockitoBean EntitlementResolver entitlements;

    @BeforeEach
    void setup() {
        when(entitlements.isPro(any())).thenReturn(true);
    }

    @Test
    void returns_dashboard_for_authed_user() {
        String userId = seedUser();
        String profileId = seedProfile(userId, "Лія");
        seedStory(userId, profileId, "Зачарований дракон");

        authedClient(userId).get().uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.isPro").isEqualTo(true)
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

    private String seedProfile(String userId, String name) {
        ChildProfile p = new ChildProfile();
        p.setId(UUID.randomUUID().toString());
        p.setUserId(userId); p.setName(name); p.setAvatarSeed("s"); p.setPreferredLanguage("uk");
        return profiles.save(p).getId();
    }

    private Story seedStory(String userId, String profileId, String title) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId); s.setChildProfileId(profileId);
        s.setTitle(title); s.setTheme("th"); s.setCharacters(List.of("c"));
        s.setAgeGroup("6-8"); s.setLength("short"); s.setLanguage("uk");
        s.setContent("body");
        return stories.save(s);
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
