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

class StoryFeaturedIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder encoder;
    @Autowired com.kazka.billing.UserEntitlementRepository entitlementRepo;

    User me;
    String bearer;

    @BeforeEach
    void clean() {
        stories.deleteAll();
        entitlementRepo.deleteAll();
        users.deleteAll();
        me = seedUser("featured@example.com");
        bearer = loginBearer("featured@example.com");
    }

    @Test
    void should_return204_when_userHasNoStories() {
        client().get().uri("/api/stories/featured")
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isNoContent();
    }

    @Test
    void should_returnMostRecentStory_when_userHasMultiple() {
        seedStory(me, "old");
        seedStory(me, "new");

        client().get().uri("/api/stories/featured")
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("new");
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
