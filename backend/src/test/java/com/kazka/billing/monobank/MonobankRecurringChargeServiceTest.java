package com.kazka.billing.monobank;

import com.kazka.billing.BillingProperties;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonobankRecurringChargeServiceTest {

    @Mock MonobankClient monobank;
    @Mock UserEntitlementRepository entitlements;
    @Mock SubscriptionProductRepository products;

    private MonobankRecurringChargeService service;
    private SimpleMeterRegistry meters;

    @BeforeEach
    void setUp() {
        meters = new SimpleMeterRegistry();
        BillingProperties.Monobank.Recurring recurring = new BillingProperties.Monobank.Recurring(
                Duration.ofHours(1), 3, "renew-");
        BillingProperties props = new BillingProperties(
                "bundle", "0", 0L, "Sandbox", "", "", "", true, 3,
                null, new BillingProperties.Monobank("tok", recurring),
                "http://localhost/success", "http://localhost/cancel");
        service = new MonobankRecurringChargeService(monobank, entitlements, products, props, meters);
    }

    @Test
    void should_chargeToken_when_entitlementDue() {
        UserEntitlement due = monobankRow(0);
        SubscriptionProduct product = newProduct();
        when(entitlements.findDueForRenewal(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(due));
        when(products.findById(due.getProductId())).thenReturn(Optional.of(product));
        when(monobank.chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.Accepted("inv_1")));

        service.tick();

        String expectedKey = "renew-" + due.getUserId() + "-" + YearMonth.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMM"));
        verify(monobank).chargeToken(eq("wallet-1"), eq("card-1"),
                eq(product.getPriceMicro()), eq(product.getCurrency()),
                eq(expectedKey), eq(expectedKey));
        assertThat(meters.counter("monobank_renewals_attempted_total", "outcome", "success").count())
                .isEqualTo(1.0);
    }

    @Test
    void should_skip_when_noEntitlementsDue() {
        when(entitlements.findDueForRenewal(any(Instant.class), any(Pageable.class))).thenReturn(List.of());

        service.tick();

        verify(monobank, never()).chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());
        assertThat(meters.counter("monobank_renewals_attempted_total", "outcome", "success").count())
                .isZero();
    }

    @Test
    void should_incrementRetryCount_when_chargeFails4xx() {
        UserEntitlement due = monobankRow(0);
        SubscriptionProduct product = newProduct();
        when(entitlements.findDueForRenewal(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(due));
        when(products.findById(due.getProductId())).thenReturn(Optional.of(product));
        when(monobank.chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.CardFailure("card expired")));

        service.tick();

        ArgumentCaptor<UserEntitlement> captor = ArgumentCaptor.forClass(UserEntitlement.class);
        verify(entitlements).save(captor.capture());
        assertThat(captor.getValue().getRenewalRetryCount()).isEqualTo(1);
        assertThat(captor.getValue().getNextRenewalAt()).isAfter(Instant.now());
        assertThat(captor.getValue().getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(meters.counter("monobank_renewals_attempted_total", "outcome", "failure").count())
                .isEqualTo(1.0);
    }

    @Test
    void should_giveUpAndNullRenewal_when_retryCountReaches3() {
        UserEntitlement due = monobankRow(2);  // 2 prior fails; this one is the 3rd
        SubscriptionProduct product = newProduct();
        when(entitlements.findDueForRenewal(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(due));
        when(products.findById(due.getProductId())).thenReturn(Optional.of(product));
        when(monobank.chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.CardFailure("card expired")));

        service.tick();

        ArgumentCaptor<UserEntitlement> captor = ArgumentCaptor.forClass(UserEntitlement.class);
        verify(entitlements).save(captor.capture());
        UserEntitlement saved = captor.getValue();
        assertThat(saved.getRenewalRetryCount()).isEqualTo(3);
        assertThat(saved.getNextRenewalAt()).isNull();
        assertThat(saved.getState()).isEqualTo(EntitlementState.ACTIVE);
    }

    @Test
    void should_leaveStateActive_during_gracePeriod() {
        UserEntitlement due = monobankRow(1);
        SubscriptionProduct product = newProduct();
        when(entitlements.findDueForRenewal(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(due));
        when(products.findById(due.getProductId())).thenReturn(Optional.of(product));
        when(monobank.chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.CardFailure("insufficient funds")));

        service.tick();

        ArgumentCaptor<UserEntitlement> captor = ArgumentCaptor.forClass(UserEntitlement.class);
        verify(entitlements).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(captor.getValue().getRenewalRetryCount()).isEqualTo(2);
    }

    @Test
    void should_skipSave_when_chargeTransient5xx() {
        UserEntitlement due = monobankRow(0);
        SubscriptionProduct product = newProduct();
        when(entitlements.findDueForRenewal(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(due));
        when(products.findById(due.getProductId())).thenReturn(Optional.of(product));
        when(monobank.chargeToken(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new MonobankChargeResult.Transient("monobank 503")));

        service.tick();

        verify(entitlements, never()).save(any(UserEntitlement.class));
        assertThat(meters.counter("monobank_renewals_attempted_total", "outcome", "transient").count())
                .isEqualTo(1.0);
    }

    private UserEntitlement monobankRow(int retries) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(UUID.randomUUID().toString());
        entitlement.setUserId(UUID.randomUUID().toString());
        entitlement.setProductId("product-1");
        entitlement.setSource(EntitlementSource.MONOBANK);
        entitlement.setState(EntitlementState.ACTIVE);
        entitlement.setExpiresAt(Instant.now().plus(Duration.ofDays(2)));
        entitlement.setNextRenewalAt(Instant.now().minusSeconds(60));
        entitlement.setMonobankWalletId("wallet-1");
        entitlement.setMonobankCardToken("card-1");
        entitlement.setRenewalRetryCount(retries);
        return entitlement;
    }

    private SubscriptionProduct newProduct() {
        SubscriptionProduct product = new SubscriptionProduct();
        product.setId("product-1");
        product.setPriceMicro(99_000_000L);
        product.setCurrency("UAH");
        product.setAppleProductId("kazka_pro_monthly");
        product.setMonobankPlanId("mono_monthly");
        product.setPeriod("P1M");
        product.setTier("pro");
        product.setName("Pro Monthly");
        return product;
    }
}
