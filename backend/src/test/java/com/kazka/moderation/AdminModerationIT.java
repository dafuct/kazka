package com.kazka.moderation;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class AdminModerationIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired FlaggedAttemptRepository flags;

    @BeforeEach
    void clean() {
        flags.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_listFlaggedAttempts_when_adminCallsModerationFlagged() {
        seedAdmin();
        seedUserWithFlag("kid@example.com", ModerationCategory.SEXUAL);

        client().get().uri("/api/admin/moderation/flagged?page=0&size=20")
                .cookie("SESSION", login("admin@example.com").sessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items[0].category").isEqualTo("SEXUAL")
                .jsonPath("$.items[0].userEmail").isEqualTo("kid@example.com");
    }

    @Test
    void should_return403_when_nonAdminCallsModerationFlagged() {
        seedUser("nobody@example.com");
        client().get().uri("/api/admin/moderation/flagged?page=0&size=20")
                .cookie("SESSION", login("nobody@example.com").sessionId())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void should_listSuspendedUsers_when_adminCallsModerationSuspended() {
        seedAdmin();
        var u = seedUser("paused@example.com");
        u.setSuspendedAt(Instant.now());
        u.setSuspendedReason("CONTENT_POLICY");
        users.save(u);

        client().get().uri("/api/admin/moderation/suspended")
                .cookie("SESSION", login("admin@example.com").sessionId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].email").isEqualTo("paused@example.com");
    }

    @Test
    void should_clearSuspensionColumns_when_adminUnsuspends() {
        seedAdmin();
        var u = seedUser("paused@example.com");
        u.setSuspendedAt(Instant.now());
        u.setSuspendedReason("CONTENT_POLICY");
        users.save(u);

        Session session = login("admin@example.com");
        client().post().uri("/api/admin/users/" + u.getId() + "/unsuspend")
                .cookie("SESSION", session.sessionId())
                .cookie("XSRF-TOKEN", session.csrfToken())
                .header("X-XSRF-TOKEN", session.csrfToken())
                .exchange()
                .expectStatus().isNoContent();

        var fresh = users.findById(u.getId()).orElseThrow();
        assertThat(fresh.isSuspended()).isFalse();
        assertThat(fresh.getSuspendedReason()).isNull();
    }

    private User seedUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setDisplayName("U");
        u.setRole(UserRole.USER);
        u.setPasswordHash(org.springframework.security.crypto.bcrypt.BCrypt.hashpw("password123", org.springframework.security.crypto.bcrypt.BCrypt.gensalt()));
        u.setEmailVerified(true);
        users.save(u);
        return u;
    }

    private void seedAdmin() {
        User u = seedUser("admin@example.com");
        u.setRole(UserRole.ADMIN);
        users.save(u);
    }

    private void seedUserWithFlag(String email, ModerationCategory cat) {
        User u = seedUser(email);
        FlaggedAttempt fa = new FlaggedAttempt();
        fa.setId(UUID.randomUUID().toString());
        fa.setUserId(u.getId());
        fa.setPipeline(ModerationPipeline.TEXT_INPUT);
        fa.setCategory(cat);
        fa.setLanguage("uk");
        fa.setPromptText("x");
        flags.save(fa);
    }

    private Session login(String email) {
        var loginResult = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class);

        ResponseCookie sessionCookie = loginResult.getResponseCookies().getFirst("SESSION");
        String sessionId = sessionCookie != null ? sessionCookie.getValue() : "";

        // Prime XSRF-TOKEN cookie via an authenticated GET (login endpoint is CSRF-excluded)
        var meResult = client().get().uri("/api/auth/me")
                .cookie("SESSION", sessionId)
                .exchange()
                .returnResult(String.class);

        MultiValueMap<String, ResponseCookie> meCookies = meResult.getResponseCookies();
        ResponseCookie csrfCookie = meCookies.getFirst("XSRF-TOKEN");
        String csrfToken = csrfCookie != null ? csrfCookie.getValue() : "";

        return new Session(sessionId, csrfToken);
    }

    record Session(String sessionId, String csrfToken) {}
}
