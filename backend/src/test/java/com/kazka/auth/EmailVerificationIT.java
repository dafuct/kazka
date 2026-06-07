package com.kazka.auth;

import com.kazka.AbstractIT;
import com.kazka.user.EmailVerificationToken;
import com.kazka.user.EmailVerificationTokenRepository;
import com.kazka.user.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository tokens;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void should_verifyUser_when_clickValidLink() throws Exception {
        signup("eva@example.com");

        String token = waitForVerificationToken();

        client().get().uri("/api/auth/verify-email?token=" + token)
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*ok=1");

        assertThat(users.findByEmail("eva@example.com")).get()
                .matches(user -> user.isEmailVerified());
    }

    @Test
    void should_redirectWithError_when_tokenInvalid() {
        client().get().uri("/api/auth/verify-email?token=does-not-exist")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*error=TOKEN_INVALID");
    }

    @Test
    void should_redirectWithError_when_tokenExpired() {
        signup("late@example.com");
        EmailVerificationToken evt = tokens.findAll().getFirst();
        evt.setExpiresAt(Instant.now().minusSeconds(60));
        tokens.save(evt);

        client().get().uri("/api/auth/verify-email?token=" + evt.getToken())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*error=TOKEN_INVALID");
    }

    private void signup(String email) {
        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123", "displayName", "Test"))
                .exchange()
                .expectStatus().isCreated();
    }

    private String waitForVerificationToken() {
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(greenMail.getReceivedMessages()).hasSizeGreaterThanOrEqualTo(1));
        MimeMessage[] received = greenMail.getReceivedMessages();
        try {
            String body = received[0].getContent().toString();
            Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
            assertThat(matcher.find()).isTrue();
            return matcher.group(1);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
