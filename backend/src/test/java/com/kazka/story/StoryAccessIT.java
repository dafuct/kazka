package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoryAccessIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        stories.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_return404_when_userBReadsUserAStory() {
        User userA = createVerifiedUser("a@example.com", "Apass123!");
        User userB = createVerifiedUser("b@example.com", "Bpass123!");
        Story story = saveStory(userA.getId(), "A's story");
        String sessionB = login("b@example.com", "Bpass123!");

        client().get().uri("/api/stories/" + story.getId())
                .header(HttpHeaders.COOKIE, sessionB)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_return200_when_adminReadsAnyStory() {
        User user = createVerifiedUser("c@example.com", "Cpass123!");
        User admin = createAdmin("admin@example.com", "Adminpass1!");
        Story story = saveStory(user.getId(), "C's story");
        String sessionAdmin = login("admin@example.com", "Adminpass1!");

        client().get().uri("/api/stories/" + story.getId())
                .header(HttpHeaders.COOKIE, sessionAdmin)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void should_listOnlyOwnStories_when_nonAdminListsArchive() {
        User userA = createVerifiedUser("la@example.com", "Apass123!");
        User userB = createVerifiedUser("lb@example.com", "Bpass123!");
        saveStory(userA.getId(), "A1");
        saveStory(userA.getId(), "A2");
        saveStory(userB.getId(), "B1");
        String sessionA = login("la@example.com", "Apass123!");

        client().get().uri("/api/stories")
                .header(HttpHeaders.COOKIE, sessionA)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(2);
    }

    private User createVerifiedUser(String email, String password) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setDisplayName(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return users.save(user);
    }

    private User createAdmin(String email, String password) {
        User user = createVerifiedUser(email, password);
        user.setRole(UserRole.ADMIN);
        return users.save(user);
    }

    private Story saveStory(String userId, String title) {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setUserId(userId);
        story.setTitle(title);
        story.setTheme("t");
        story.setCharacters(List.of("hero"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("body");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        return stories.save(story);
    }

    private String login(String email, String password) {
        var response = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class);
        ResponseCookie sessionCookie = response.getResponseCookies().getFirst("SESSION");
        assertThat(sessionCookie).isNotNull();
        return "SESSION=" + sessionCookie.getValue();
    }
}
