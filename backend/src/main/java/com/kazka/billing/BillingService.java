package com.kazka.billing;

import com.apple.itunes.storekit.model.Data;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.NotificationTypeV2;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.model.Subtype;
import com.kazka.billing.paypro.PayProClient;
import com.kazka.billing.webhook.WebhookIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BillingService {

    private final IapVerifier verifier;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;
    private final WebhookIdempotencyService idempotency;
    private final ApplicationEventPublisher events;
    private final PayProClient payProClient;

    @Transactional
    public Mono<UserEntitlement> verifyAndPersist(String userId, String signedTransaction) {
        return Mono.fromCallable(() -> {
            JWSTransactionDecodedPayload payload = verifier.verifyTransaction(signedTransaction);
            SubscriptionProduct product = products.findByAppleProductId(payload.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Unknown product: " + payload.getProductId()));

            String origTxn = Objects.requireNonNull(
                    payload.getOriginalTransactionId(),
                    "originalTransactionId must not be null");
            UserEntitlement entitlement = entitlements.findByOriginalTransactionId(origTxn)
                    .orElseGet(() -> {
                        UserEntitlement newEntitlement = new UserEntitlement();
                        newEntitlement.setId(UUID.randomUUID().toString());
                        newEntitlement.setUserId(userId);
                        newEntitlement.setProductId(product.getId());
                        newEntitlement.setOriginalTransactionId(origTxn);
                        newEntitlement.setSource(EntitlementSource.APPLE);
                        return newEntitlement;
                    });
            entitlement.setState(EntitlementState.ACTIVE);
            entitlement.setExpiresAt(payload.getExpiresDate() == null
                    ? null
                    : Instant.ofEpochMilli(payload.getExpiresDate()));
            entitlement.setLatestJws(signedTransaction);
            return entitlements.save(entitlement);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<Void> ingestWebhook(String signedPayload) {
        return Mono.<Void>fromCallable(() -> {
            ResponseBodyV2DecodedPayload payload = verifier.verifyNotification(signedPayload);
            NotificationTypeV2 type = payload.getNotificationType();
            Subtype subtype = payload.getSubtype();
            log.info("ASN V2 ingest: type={} subtype={}", type, subtype);

            // Replay window: Apple's signedDate is included in every notification.
            // Reject anything outside ±10 minutes of "now" to block replay of captured events.
            Long signedDate = payload.getSignedDate();
            if (signedDate != null) {
                Instant ts = Instant.ofEpochMilli(signedDate);
                Instant now = Instant.now();
                if (ts.isBefore(now.minusSeconds(600)) || ts.isAfter(now.plusSeconds(600))) {
                    log.warn("ASN V2 stale/future signedDate={} (now={}); ignoring", ts, now);
                    return null;
                }
            }

            // notificationUUID is the per-event id Apple guarantees stable across retries.
            String notificationUuid = payload.getNotificationUUID();
            if (!idempotency.markProcessed("apple", notificationUuid)) {
                return null;
            }

            String signedTxn = Optional.ofNullable(payload.getData())
                    .map(Data::getSignedTransactionInfo)
                    .orElse(null);
            if (signedTxn == null) {
                log.warn("ASN V2 has no signedTransactionInfo; ignoring");
                return null;
            }
            JWSTransactionDecodedPayload txn = verifier.verifyTransaction(signedTxn);
            String origTxn = Objects.requireNonNull(
                    txn.getOriginalTransactionId(),
                    "originalTransactionId must not be null").toString();
            UserEntitlement entitlement = entitlements.findByOriginalTransactionId(origTxn).orElse(null);
            if (entitlement == null) {
                log.warn("Webhook references unknown originalTransactionId={}; ignoring", origTxn);
                return null;
            }
            Optional<EntitlementState> nextState = mapNotificationType(type, subtype);
            if (nextState.isEmpty()) {
                log.warn("ASN V2 type={} subtype={} — no state mapping; leaving entitlement {} unchanged",
                        type, subtype, entitlement.getId());
                return null;
            }
            EntitlementState newState = nextState.get();
            entitlement.setState(newState);
            if (txn.getExpiresDate() != null) {
                entitlement.setExpiresAt(Instant.ofEpochMilli(txn.getExpiresDate()));
            }
            entitlement.setLatestJws(signedTxn);
            entitlements.save(entitlement);
            if (newState != EntitlementState.ACTIVE && newState != EntitlementState.GRACE) {
                events.publishEvent(new EntitlementDowngradedEvent(entitlement.getUserId()));
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static Optional<EntitlementState> mapNotificationType(NotificationTypeV2 type, Subtype subtype) {
        return switch (type) {
            case DID_RENEW, SUBSCRIBED -> Optional.of(EntitlementState.ACTIVE);
            case EXPIRED, GRACE_PERIOD_EXPIRED -> Optional.of(EntitlementState.EXPIRED);
            case DID_FAIL_TO_RENEW -> Optional.of(subtype == Subtype.GRACE_PERIOD
                    ? EntitlementState.GRACE
                    : EntitlementState.EXPIRED);
            case REFUND, REVOKE -> Optional.of(EntitlementState.REFUNDED);
            default -> Optional.empty();
        };
    }

    @Transactional
    public Mono<List<UserEntitlement>> revokeActiveForUser(String userId) {
        return Mono.fromCallable(() -> {
            List<UserEntitlement> active = entitlements.findByUserId(userId).stream()
                    .filter(entitlement -> entitlement.getState() == EntitlementState.ACTIVE
                            || entitlement.getState() == EntitlementState.GRACE)
                    .toList();
            if (active.isEmpty()) {
                return active;
            }
            boolean appleManaged = active.stream()
                    .anyMatch(entitlement -> entitlement.getSource() == EntitlementSource.APPLE);
            if (appleManaged) {
                throw new AppleManagedSubscriptionException();
            }
            boolean anyDowngraded = false;
            for (UserEntitlement entitlement : active) {
                switch (entitlement.getSource()) {
                    case MONOBANK -> {
                        // Stop the scheduler but keep the active period intact.
                        // Card token is NOT cleared so the user can re-subscribe without re-entry.
                        entitlement.setNextRenewalAt(null);
                        entitlements.save(entitlement);
                    }
                    case PAYPRO -> {
                        // Call PayPro API first — local state must reflect provider state.
                        // If the API throws, the row stays ACTIVE so the user sees a real error
                        // instead of "cancelled in UI but still being billed".
                        payProClient.terminate(entitlement.getOriginalTransactionId()).block();
                        entitlement.setState(EntitlementState.REVOKED);
                        entitlement.setExpiresAt(Instant.now());
                        entitlements.save(entitlement);
                        anyDowngraded = true;
                    }
                    case GIFT -> {
                        entitlement.setState(EntitlementState.REVOKED);
                        entitlement.setExpiresAt(Instant.now());
                        entitlements.save(entitlement);
                        anyDowngraded = true;
                    }
                    case APPLE -> { /* unreachable — handled above */ }
                }
            }
            if (anyDowngraded) {
                events.publishEvent(new EntitlementDowngradedEvent(userId));
            }
            return active;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<UserEntitlement>> findActive(String userId) {
        return Mono.fromCallable(() -> entitlements.findByUserId(userId).stream()
                        .filter(entitlement -> entitlement.getState() == EntitlementState.ACTIVE || entitlement.getState() == EntitlementState.GRACE)
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<SubscriptionProduct>> listProducts() {
        return Mono.fromCallable(products::findAll)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<SubscriptionProduct> findProductById(String productId) {
        return Mono.fromCallable(() -> products.findById(productId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }
}
