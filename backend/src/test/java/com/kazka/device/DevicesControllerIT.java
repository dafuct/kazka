package com.kazka.device;

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

class DevicesControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired DeviceTokenRepository devices;
    @Autowired PasswordEncoder encoder;
    @Autowired com.kazka.billing.UserEntitlementRepository entitlementRepo;

    String bearer;

    @BeforeEach
    void clean() {
        devices.deleteAll();
        entitlementRepo.deleteAll();
        users.deleteAll();
        seedUser("dev-user@example.com");
        bearer = loginBearer("dev-user@example.com");
    }

    @Test
    void should_persistRow_when_registerCalledWithFreshToken() {
        client().post().uri("/api/devices/register")
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("deviceToken", "abc123", "platform", "ios", "locale", "uk"))
                .exchange().expectStatus().isNoContent();

        var row = devices.findByDeviceToken("abc123");
        assertThat(row).isPresent();
        assertThat(row.get().getPlatform()).isEqualTo("ios");
        assertThat(row.get().getLocale()).isEqualTo("uk");
    }

    @Test
    void should_upsertRow_when_tokenRegisteredTwice() {
        client().post().uri("/api/devices/register")
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("deviceToken", "abc123", "platform", "ios", "locale", "uk"))
                .exchange().expectStatus().isNoContent();

        client().post().uri("/api/devices/register")
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("deviceToken", "abc123", "platform", "ios", "locale", "en"))
                .exchange().expectStatus().isNoContent();

        assertThat(devices.findAll()).hasSize(1);
        assertThat(devices.findByDeviceToken("abc123").orElseThrow().getLocale()).isEqualTo("en");
    }

    @Test
    void should_deleteRow_when_unregisterCalled() {
        client().post().uri("/api/devices/register")
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("deviceToken", "to-delete", "platform", "ios"))
                .exchange().expectStatus().isNoContent();

        client().delete().uri("/api/devices/to-delete")
                .header("Authorization", "Bearer " + bearer)
                .exchange().expectStatus().isNoContent();

        assertThat(devices.findByDeviceToken("to-delete")).isEmpty();
    }

    @Test
    void should_return401_when_noAuth() {
        client().post().uri("/api/devices/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("deviceToken", "x", "platform", "ios"))
                .exchange().expectStatus().isUnauthorized();
    }

    private void seedUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("password123"));
        u.setDisplayName("Tester");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        users.save(u);
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
