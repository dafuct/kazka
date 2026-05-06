package com.kazka.auth;

import com.kazka.AbstractIT;
import com.kazka.user.PasswordResetToken;
import com.kazka.user.PasswordResetTokenRepository;
import com.kazka.user.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() throws Exception {
        users.deleteAll();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Test
    void should_return204_when_requestPasswordResetForUnknownEmail() {
        client().post().uri("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "nobody@example.com"))
                .exchange()
                .expectStatus().isNoContent();
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void should_sendResetEmail_when_requestForKnownEmail() throws Exception {
        signup("real@example.com");
        greenMail.purgeEmailFromAllMailboxes();

        client().post().uri("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "real@example.com"))
                .exchange()
                .expectStatus().isNoContent();

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(1));
    }

    @Test
    void should_updatePasswordHash_when_confirmWithValidToken() throws Exception {
        signup("change@example.com");
        client().post().uri("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "change@example.com"))
                .exchange().expectStatus().isNoContent();

        String token = waitForResetToken();

        client().post().uri("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("token", token, "newPassword", "newpassword456"))
                .exchange()
                .expectStatus().isNoContent();

        var user = users.findByEmail("change@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("newpassword456", user.getPasswordHash())).isTrue();
        PasswordResetToken used = resetTokens.findById(token).orElseThrow();
        assertThat(used.getConsumedAt()).isNotNull();
    }

    @Test
    void should_return400_when_confirmWithUnknownToken() {
        client().post().uri("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("token", "missing", "newPassword", "newpassword456"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("TOKEN_INVALID");
    }

    private void signup(String email) {
        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123", "displayName", "User"))
                .exchange().expectStatus().isCreated();
    }

    private String waitForResetToken() {
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSizeGreaterThanOrEqualTo(1));
        try {
            MimeMessage[] msgs = greenMail.getReceivedMessages();
            String body = msgs[msgs.length - 1].getContent().toString();
            Matcher m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
            assertThat(m.find()).isTrue();
            return m.group(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
