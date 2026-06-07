package com.kazka.child.bedtime;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementResolver;
import com.kazka.child.ChildProfile;
import com.kazka.child.ChildProfileRepository;
import com.kazka.child.bedtime.dto.BedtimeUpdateRequest;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("integration")
class BedtimeScheduleControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired ChildProfileRepository profiles;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EntitlementResolver entitlements;

    String userA;
    String userB;
    String profileA;

    @BeforeEach
    void seed() {
        when(entitlements.isPro(any())).thenReturn(true);
        userA = seedUser();
        userB = seedUser();
        profileA = seedProfile(userA);
    }

    @Test
    void should_return_empty_schedule_when_never_configured() {
        var client = authedClient(userA);
        client.get().uri("/api/children/" + profileA + "/bedtime").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.enabled").isEqualTo(false)
                .jsonPath("$.localTime").isEqualTo("20:30")
                .jsonPath("$.timezone").isEqualTo("Europe/Kyiv");
    }

    @Test
    void should_upsert_and_then_return_persisted_values() {
        var client = authedClient(userA);
        client.put().uri("/api/children/" + profileA + "/bedtime")
                .bodyValue(new BedtimeUpdateRequest(true, "21:00", "Europe/Warsaw", List.of("dragons"), true))
                .exchange().expectStatus().isOk();

        client.get().uri("/api/children/" + profileA + "/bedtime").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.enabled").isEqualTo(true)
                .jsonPath("$.localTime").isEqualTo("21:00")
                .jsonPath("$.timezone").isEqualTo("Europe/Warsaw")
                .jsonPath("$.themes[0]").isEqualTo("dragons")
                .jsonPath("$.nextRunAt").isNotEmpty();
    }

    @Test
    void should_return402_when_freeTier_enables() {
        when(entitlements.isPro(userA)).thenReturn(false);
        var client = authedClient(userA);
        client.put().uri("/api/children/" + profileA + "/bedtime")
                .bodyValue(new BedtimeUpdateRequest(true, "20:30", "Europe/Kyiv", List.of(), true))
                .exchange().expectStatus().isEqualTo(402);
    }

    @Test
    void should_return400_on_bad_timezone() {
        var client = authedClient(userA);
        client.put().uri("/api/children/" + profileA + "/bedtime")
                .bodyValue(new BedtimeUpdateRequest(true, "20:30", "Mars/Olympus", List.of(), true))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void should_return404_when_accessing_another_users_profile() {
        var clientB = authedClient(userB);
        clientB.get().uri("/api/children/" + profileA + "/bedtime")
                .exchange().expectStatus().isNotFound();
        clientB.put().uri("/api/children/" + profileA + "/bedtime")
                .bodyValue(new BedtimeUpdateRequest(false, "20:30", "Europe/Kyiv", List.of(), true))
                .exchange().expectStatus().isNotFound();
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test");
        user.setDisplayName("T");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);
        return id;
    }

    private String seedProfile(String userId) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId); profile.setName("T"); profile.setAvatarSeed("s"); profile.setPreferredLanguage("uk");
        return profiles.save(profile).getId();
    }

    /**
     * Obtains a Bearer access token via /api/auth/token/login and a CSRF token from
     * a safe GET, then returns a WebTestClient pre-configured with both.
     * The CSRF cookie+header pair is required for mutating requests (POST/PATCH/DELETE/PUT)
     * that are not in the CSRF exclusion list.
     */
    private WebTestClient authedClient(String userId) {
        String email = users.findById(userId).orElseThrow().getEmail();
        @SuppressWarnings("rawtypes")
        Map body = client().post().uri("/api/auth/token/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class).getResponseBody().blockFirst();
        String bearer = body.get("accessToken").toString();

        // Fetch CSRF token from any safe GET endpoint
        EntityExchangeResult<byte[]> csrf = client()
                .get().uri("/api/billing/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult();
        MultiValueMap<String, ResponseCookie> csrfCookies = csrf.getResponseCookies();
        ResponseCookie xsrfCookie = csrfCookies.getFirst("XSRF-TOKEN");
        if (xsrfCookie == null) {
            throw new IllegalStateException("No XSRF-TOKEN cookie issued by server");
        }
        String csrfToken = xsrfCookie.getValue();

        return client().mutate()
                .defaultHeader("Authorization", "Bearer " + bearer)
                .defaultHeader("X-XSRF-TOKEN", csrfToken)
                .defaultCookie("XSRF-TOKEN", csrfToken)
                .build();
    }
}
