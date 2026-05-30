package com.kazka.billing;

import com.kazka.billing.UserEntitlementRepository.EntitlementSummary;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementResolverTest {

    @Mock UserEntitlementRepository entitlements;
    @Mock UserRepository users;
    @InjectMocks EntitlementResolver resolver;

    @BeforeEach
    void defaultUserIsRegular() {
        lenient().when(users.findById(anyString())).thenReturn(Optional.of(user(UserRole.USER)));
    }

    @Test
    void should_return_true_when_active_and_unexpired() {
        when(entitlements.findSummariesByUserId(anyString()))
                .thenReturn(List.of(summary(EntitlementState.ACTIVE, Instant.now().plusSeconds(60))));
        assertThat(resolver.isPro("u1")).isTrue();
    }

    @Test
    void should_return_false_when_active_but_expired() {
        when(entitlements.findSummariesByUserId(anyString()))
                .thenReturn(List.of(summary(EntitlementState.ACTIVE, Instant.now().minusSeconds(60))));
        assertThat(resolver.isPro("u1")).isFalse();
    }

    @Test
    void should_return_true_when_in_grace_regardless_of_expiry() {
        when(entitlements.findSummariesByUserId(anyString()))
                .thenReturn(List.of(summary(EntitlementState.GRACE, Instant.now().minusSeconds(60))));
        assertThat(resolver.isPro("u1")).isTrue();
    }

    @Test
    void should_return_false_when_refunded() {
        when(entitlements.findSummariesByUserId(anyString()))
                .thenReturn(List.of(summary(EntitlementState.REFUNDED, Instant.now().plusSeconds(60))));
        assertThat(resolver.isPro("u1")).isFalse();
    }

    @Test
    void should_return_false_when_no_entitlements() {
        when(entitlements.findSummariesByUserId(anyString())).thenReturn(List.of());
        assertThat(resolver.isPro("u1")).isFalse();
    }

    @Test
    void should_return_true_for_admin_without_entitlements() {
        when(users.findById(anyString())).thenReturn(Optional.of(user(UserRole.ADMIN)));
        assertThat(resolver.isPro("admin1")).isTrue();
    }

    private static EntitlementSummary summary(EntitlementState state, Instant expires) {
        return new EntitlementSummary() {
            @Override public EntitlementState getState() { return state; }
            @Override public Instant getExpiresAt() { return expires; }
        };
    }

    private static User user(UserRole role) {
        User u = new User();
        u.setRole(role);
        return u;
    }
}
