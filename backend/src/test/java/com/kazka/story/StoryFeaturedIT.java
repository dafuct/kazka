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

    User me;
    String bearer;

    @BeforeEach
    void clean() {
        stories.deleteAll();
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
