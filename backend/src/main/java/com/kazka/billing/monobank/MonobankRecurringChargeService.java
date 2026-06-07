package com.kazka.billing.monobank;

import com.kazka.billing.BillingProperties;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class MonobankRecurringChargeService {

    private static final int BATCH_SIZE = 100;
    private static final DateTimeFormatter YYYY_MM = DateTimeFormatter.ofPattern("yyyyMM");

    private final MonobankClient monobank;
    private final UserEntitlementRepository entitlements;
    private final SubscriptionProductRepository products;
    private final BillingProperties props;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter transientCounter;

    public MonobankRecurringChargeService(MonobankClient monobank,
                                          UserEntitlementRepository entitlements,
                                          SubscriptionProductRepository products,
                                          BillingProperties props,
                                          MeterRegistry meters) {
        this.monobank = monobank;
        this.entitlements = entitlements;
        this.products = products;
        this.props = props;
        this.successCounter = Counter.builder("monobank_renewals_attempted_total")
                .tag("outcome", "success").register(meters);
        this.failureCounter = Counter.builder("monobank_renewals_attempted_total")
                .tag("outcome", "failure").register(meters);
        this.transientCounter = Counter.builder("monobank_renewals_attempted_total")
                .tag("outcome", "transient").register(meters);
    }

    @Scheduled(fixedDelayString = "${kazka.billing.monobank.recurring.tick-interval}")
    @Transactional
    public void tick() {
        Instant now = Instant.now();
        List<UserEntitlement> due = entitlements.findDueForRenewal(now, PageRequest.of(0, BATCH_SIZE));
        if (due.isEmpty()) return;
        log.info("Monobank recurring tick: {} entitlements due", due.size());
        for (UserEntitlement entitlement : due) {
            chargeOne(entitlement);
        }
    }

    private void chargeOne(UserEntitlement entitlement) {
        SubscriptionProduct product = products.findById(entitlement.getProductId()).orElse(null);
        if (product == null) {
            log.warn("Recurring charge: no product for entitlement {}", entitlement.getId());
            return;
        }
        String key = idempotencyKey(entitlement.getUserId());
        MonobankChargeResult result = monobank.chargeToken(
                entitlement.getMonobankWalletId(), entitlement.getMonobankCardToken(),
                product.getPriceMicro(), product.getCurrency(),
                key, key).block();
        if (result instanceof MonobankChargeResult.Accepted) {
            successCounter.increment();
            // Webhook will land the actual entitlement update; we leave the row alone.
        } else if (result instanceof MonobankChargeResult.CardFailure) {
            failureCounter.increment();
            int max = props.monobank().recurring() != null
                    ? props.monobank().recurring().graceMaxRetries() : 3;
            int retries = entitlement.getRenewalRetryCount() + 1;
            entitlement.setRenewalRetryCount(retries);
            if (retries < max) {
                entitlement.setNextRenewalAt(Instant.now().plus(Duration.ofDays(1)));
            } else {
                entitlement.setNextRenewalAt(null);
            }
            // State stays ACTIVE — user keeps Pro until expires_at.
            entitlements.save(entitlement);
        } else if (result instanceof MonobankChargeResult.Transient transientResult) {
            transientCounter.increment();
            log.warn("Recurring charge transient failure for entitlement {}: {}", entitlement.getId(), transientResult.reason());
        }
    }

    private String idempotencyKey(String userId) {
        String prefix = props.monobank().recurring() != null
                ? props.monobank().recurring().idempotencyPrefix() : "renew-";
        return prefix + userId + "-" + YearMonth.now(ZoneOffset.UTC).format(YYYY_MM);
    }
}
