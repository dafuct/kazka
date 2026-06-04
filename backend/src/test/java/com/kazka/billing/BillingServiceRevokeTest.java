package com.kazka.billing;

import com.kazka.billing.webhook.WebhookIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceRevokeTest {

    @Mock IapVerifier verifier;
    @Mock SubscriptionProductRepository products;
    @Mock UserEntitlementRepository entitlements;
    @Mock WebhookIdempotencyService idempotency;
    @Mock ApplicationEventPublisher events;

    private BillingService service;

    @BeforeEach
    void setUp() {
        service = new BillingService(verifier, products, entitlements, idempotency, events);
    }

    @Test
    void should_throwAppleManaged_when_appleSourceActive() {
        UserEntitlement apple = entitlement(EntitlementSource.APPLE);
        when(entitlements.findByUserId("u1")).thenReturn(List.of(apple));

        StepVerifier.create(service.revokeActiveForUser("u1"))
                .expectError(AppleManagedSubscriptionException.class)
                .verify();
    }

    @Test
    void should_stopRenewalButKeepActive_when_monobankSource() {
        UserEntitlement mono = entitlement(EntitlementSource.MONOBANK);
        mono.setNextRenewalAt(Instant.now().plus(Duration.ofDays(20)));
        Instant originalExpires = mono.getExpiresAt();
        when(entitlements.findByUserId("u1")).thenReturn(List.of(mono));

        StepVerifier.create(service.revokeActiveForUser("u1")).expectNextCount(1).verifyComplete();

        ArgumentCaptor<UserEntitlement> captor = ArgumentCaptor.forClass(UserEntitlement.class);
        verify(entitlements).save(captor.capture());
        UserEntitlement saved = captor.getValue();
        assertThat(saved.getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(saved.getNextRenewalAt()).isNull();
        assertThat(saved.getExpiresAt()).isEqualTo(originalExpires);
        assertThat(saved.getMonobankCardToken()).isEqualTo("card-1");  // preserved
    }

    @Test
    void should_hardRevoke_when_paddleSource() {
        UserEntitlement paddle = entitlement(EntitlementSource.PADDLE);
        when(entitlements.findByUserId("u1")).thenReturn(List.of(paddle));

        StepVerifier.create(service.revokeActiveForUser("u1")).expectNextCount(1).verifyComplete();

        ArgumentCaptor<UserEntitlement> captor = ArgumentCaptor.forClass(UserEntitlement.class);
        verify(entitlements).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(EntitlementState.REVOKED);
    }

    @Test
    void should_hardRevoke_when_giftSource() {
        UserEntitlement gift = entitlement(EntitlementSource.GIFT);
        when(entitlements.findByUserId("u1")).thenReturn(List.of(gift));

        StepVerifier.create(service.revokeActiveForUser("u1")).expectNextCount(1).verifyComplete();

        ArgumentCaptor<UserEntitlement> captor = ArgumentCaptor.forClass(UserEntitlement.class);
        verify(entitlements).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(EntitlementState.REVOKED);
    }

    private UserEntitlement entitlement(EntitlementSource source) {
        UserEntitlement e = new UserEntitlement();
        e.setId(UUID.randomUUID().toString());
        e.setUserId("u1");
        e.setSource(source);
        e.setState(EntitlementState.ACTIVE);
        e.setExpiresAt(Instant.now().plus(Duration.ofDays(20)));
        e.setMonobankCardToken("card-1");
        return e;
    }
}
