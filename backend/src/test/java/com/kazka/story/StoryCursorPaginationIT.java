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

import static org.assertj.core.api.Assertions.assertThat;

class StoryCursorPaginationIT extends AbstractIT {

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
        me = seedUser("paginator@example.com");
        bearer = loginBearer("paginator@example.com");
    }

    @Test
    void should_pageThrough15Stories_when_cursorWalkedToEnd() {
        for (int i = 0; i < 15; i++) seedStory(me, "title-" + i);

        @SuppressWarnings("rawtypes")
        Map page1 = client().get().uri("/api/stories/cursor?limit=5")
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        assertThat(((java.util.List<?>) page1.get("items"))).hasSize(5);
        String c1 = (String) page1.get("nextCursor");
        assertThat(c1).isNotBlank();

        @SuppressWarnings("rawtypes")
        Map page2 = client().get().uri("/api/stories/cursor?limit=5&cursor=" + c1)
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        assertThat(((java.util.List<?>) page2.get("items"))).hasSize(5);
        String c2 = (String) page2.get("nextCursor");
        assertThat(c2).isNotBlank().isNotEqualTo(c1);

        @SuppressWarnings("rawtypes")
        Map page3 = client().get().uri("/api/stories/cursor?limit=5&cursor=" + c2)
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        assertThat(((java.util.List<?>) page3.get("items"))).hasSize(5);
        assertThat(page3.get("nextCursor")).isNull();
    }

    @Test
    void should_returnAllItemsAndNullCursor_when_libraryFitsInOnePage() {
        for (int i = 0; i < 3; i++) seedStory(me, "only-" + i);

        @SuppressWarnings("rawtypes")
        Map only = client().get().uri("/api/stories/cursor?limit=20")
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        assertThat(((java.util.List<?>) only.get("items"))).hasSize(3);
        assertThat(only.get("nextCursor")).isNull();
    }

    @Test
    void should_return400_when_cursorIsCorrupt() {
        client().get().uri("/api/stories/cursor?cursor=not-a-valid-base64")
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().is4xxClientError();
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
