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

    @BeforeEach
    void clean() {
        stories.deleteAll();
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
        Story story = seedStory(me, "fetch-me");
        String bearer = loginBearer("bearer-get@example.com");

        client().get().uri("/api/stories/" + story.getId())
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(story.getId());
    }

    @Test
    void should_return401_when_noBearer() {
        client().get().uri("/api/stories")
                .exchange().expectStatus().isUnauthorized();
    }

    private User seedUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode("password123"));
        user.setDisplayName("Tester");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }

    private Story seedStory(User owner, String title) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(owner.getId());
        story.setTitle(title);
        story.setTheme("test theme");
        story.setCharacters(java.util.List.of("Friend"));
        story.setAgeGroup("3-5");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("content body");
        story.setIllustrationStatus(IllustrationStatus.READY);
        return stories.save(story);
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
