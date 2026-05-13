package com.kazka.billing;

import com.apple.itunes.storekit.model.Data;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.NotificationTypeV2;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.model.Subtype;
import com.apple.itunes.storekit.verification.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
    public UserEntitlement verifyAndPersist(String userId, String signedTransaction) throws VerificationException {
        JWSTransactionDecodedPayload payload = verifier.verifyTransaction(signedTransaction);
        SubscriptionProduct product = products.findByAppleProductId(payload.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown product: " + payload.getProductId()));

        String origTxn = String.valueOf(payload.getOriginalTransactionId());
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
    }

    @Transactional
    public void ingestWebhook(String signedPayload) throws VerificationException {
        ResponseBodyV2DecodedPayload payload = verifier.verifyNotification(signedPayload);
        NotificationTypeV2 type = payload.getNotificationType();
        Subtype subtype = payload.getSubtype();
        log.info("ASN V2 ingest: type={} subtype={}", type, subtype);

        String signedTxn = Optional.ofNullable(payload.getData())
                .map(Data::getSignedTransactionInfo)
                .orElse(null);
        if (signedTxn == null) {
            log.warn("ASN V2 has no signedTransactionInfo; ignoring");
            return;
        }
        JWSTransactionDecodedPayload txn = verifier.verifyTransaction(signedTxn);
        String origTxn = String.valueOf(txn.getOriginalTransactionId());
        UserEntitlement e = entitlements.findByOriginalTransactionId(origTxn).orElse(null);
        if (e == null) {
            log.warn("Webhook references unknown originalTransactionId={}; ignoring", origTxn);
            return;
        }
        e.setState(mapNotificationType(type, subtype));
        if (txn.getExpiresDate() != null) {
            e.setExpiresAt(Instant.ofEpochMilli(txn.getExpiresDate()));
        }
        e.setLatestJws(signedTxn);
        entitlements.save(e);
    }

    private static EntitlementState mapNotificationType(NotificationTypeV2 type, Subtype subtype) {
        return switch (type) {
            case DID_RENEW, SUBSCRIBED -> EntitlementState.ACTIVE;
            case EXPIRED, GRACE_PERIOD_EXPIRED -> EntitlementState.EXPIRED;
            case DID_FAIL_TO_RENEW -> subtype == Subtype.GRACE_PERIOD
                    ? EntitlementState.GRACE
                    : EntitlementState.EXPIRED;
            case REFUND, REVOKE -> EntitlementState.REFUNDED;
            default -> EntitlementState.ACTIVE;
        };
    }

    public List<UserEntitlement> findActive(String userId) {
        return entitlements.findByUserId(userId).stream()
                .filter(e -> e.getState() == EntitlementState.ACTIVE || e.getState() == EntitlementState.GRACE)
                .toList();
    }

    public List<SubscriptionProduct> listProducts() {
        return products.findAll();
    }

    public Optional<SubscriptionProduct> findProductById(String productId) {
        return products.findById(productId);
    }
}
