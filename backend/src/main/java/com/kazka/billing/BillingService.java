package com.kazka.billing;

import com.apple.itunes.storekit.model.Data;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.NotificationTypeV2;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.model.Subtype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final IapVerifier verifier;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;

    public BillingService(IapVerifier verifier,
                          SubscriptionProductRepository products,
                          UserEntitlementRepository entitlements) {
        this.verifier = verifier;
        this.products = products;
        this.entitlements = entitlements;
    }

    @Transactional
    public Mono<UserEntitlement> verifyAndPersist(String userId, String signedTransaction) {
        return Mono.fromCallable(() -> {
            JWSTransactionDecodedPayload payload = verifier.verifyTransaction(signedTransaction);
            SubscriptionProduct product = products.findByAppleProductId(payload.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Unknown product: " + payload.getProductId()));

            String origTxn = Objects.requireNonNull(
                    payload.getOriginalTransactionId(),
                    "originalTransactionId must not be null").toString();
            UserEntitlement entitlement = entitlements.findByOriginalTransactionId(origTxn)
                    .orElseGet(() -> {
                        UserEntitlement e = new UserEntitlement();
                        e.setId(UUID.randomUUID().toString());
                        e.setUserId(userId);
                        e.setProductId(product.getId());
                        e.setOriginalTransactionId(origTxn);
                        return e;
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
            UserEntitlement e = entitlements.findByOriginalTransactionId(origTxn).orElse(null);
            if (e == null) {
                log.warn("Webhook references unknown originalTransactionId={}; ignoring", origTxn);
                return null;
            }
            Optional<EntitlementState> nextState = mapNotificationType(type, subtype);
            if (nextState.isEmpty()) {
                log.warn("ASN V2 type={} subtype={} — no state mapping; leaving entitlement {} unchanged",
                        type, subtype, e.getId());
                return null;
            }
            e.setState(nextState.get());
            if (txn.getExpiresDate() != null) {
                e.setExpiresAt(Instant.ofEpochMilli(txn.getExpiresDate()));
            }
            e.setLatestJws(signedTxn);
            entitlements.save(e);
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

    public Mono<List<UserEntitlement>> findActive(String userId) {
        return Mono.fromCallable(() -> entitlements.findByUserId(userId).stream()
                        .filter(e -> e.getState() == EntitlementState.ACTIVE || e.getState() == EntitlementState.GRACE)
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
