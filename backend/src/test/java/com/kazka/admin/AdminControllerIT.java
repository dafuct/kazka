package com.kazka.admin;

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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() { users.deleteAll(); }

    @Test
    void should_return403_when_nonAdminListsUsers() {
        seed("u@example.com", "Userpass1!", UserRole.USER);
        String session = login("u@example.com", "Userpass1!");

        client().get().uri("/api/admin/users")
                .header(HttpHeaders.COOKIE, session)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void should_returnUsersWithoutPasswordHash_when_adminListsUsers() {
        seed("a@example.com", "Adminpass1!", UserRole.ADMIN);
        seed("u@example.com", "Userpass1!", UserRole.USER);
        String session = login("a@example.com", "Adminpass1!");

        client().get().uri("/api/admin/users")
                .header(HttpHeaders.COOKIE, session)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].passwordHash").doesNotExist()
                .jsonPath("$[0].googleSubject").doesNotExist();
    }

    private void seed(String email, String password, UserRole role) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setDisplayName(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        u.setEmailVerified(true);
        users.save(u);
    }

    private String login(String email, String password) {
        var r = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class);
        ResponseCookie c = r.getResponseCookies().getFirst("SESSION");
        assertThat(c).isNotNull();
        return "SESSION=" + c.getValue();
    }
}
