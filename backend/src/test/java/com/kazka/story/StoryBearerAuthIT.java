package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

class StoryBearerAuthIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder encoder;
    @Autowired com.kazka.billing.UserEntitlementRepository entitlementRepo;

    @BeforeEach
    void clean() {
        stories.deleteAll();
        entitlementRepo.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_listStoriesViaBearer() {
        User me = seedUser("bearer-list@example.com");
        seedStory(me, "via-bearer");
        String bearer = loginBearer("bearer-list@example.com");

        client().get().uri("/api/stories")
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.items[0].title").isEqualTo("via-bearer");
    }

    @Test
    void should_getStoryByIdViaBearer() {
        User me = seedUser("bearer-get@example.com");
        Story s = seedStory(me, "fetch-me");
        String bearer = loginBearer("bearer-get@example.com");

        client().get().uri("/api/stories/" + s.getId())
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(s.getId());
    }

    @Test
    void should_return401_when_noBearer() {
        client().get().uri("/api/stories")
                .exchange().expectStatus().isUnauthorized();
    }

    private User seedUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("password123"));
        u.setDisplayName("Tester");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return users.save(u);
    }

    private Story seedStory(User user, String title) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(user.getId());
        s.setTitle(title);
        s.setTheme("test theme");
        s.setCharacters(java.util.List.of("Friend"));
        s.setAgeGroup("3-5");
        s.setLength("short");
        s.setLanguage("uk");
        s.setContent("content body");
        s.setIllustrationStatus(IllustrationStatus.READY);
        return stories.save(s);
    }

    private String loginBearer(String email) {
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        return body.get("accessToken").toString();
    }
}
