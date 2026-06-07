package com.kazka.billing.monobank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.billing.BillingProperties;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.billing.webhook.WebhookIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/billing/webhook")
public class MonobankWebhookController {

    private static final Duration PERIOD = Duration.ofDays(30);
    private static final Duration RENEW_LEAD = Duration.ofDays(1);

    private final BillingProperties props;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;
    private final WebhookIdempotencyService idempotency;
    private final MonobankPubKeyService pubKeyService;
    private final ObjectMapper json = new ObjectMapper();

    @PostMapping(path = "/monobank", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> handle(@RequestHeader(name = "X-Sign", required = false) String sign,
                             @RequestBody String body) {
        if (sign == null || sign.isBlank()) {
            log.warn("Monobank webhook missing X-Sign; ignoring");
            return Mono.empty();
        }
        return pubKeyService.publicKey()
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("Monobank webhook received but pubkey unavailable; ignoring")))
                .flatMap(pk -> {
                    if (!verify(pk, body, sign)) {
                        // Key may have rotated; drop the cache so the next webhook re-fetches.
                        pubKeyService.invalidate();
                        log.warn("Monobank webhook X-Sign verification failed; ignoring");
                        return Mono.empty();
                    }
                    return Mono.fromRunnable(() -> process(body))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                });
    }

    private static boolean verify(PublicKey pk, String body, String signBase64) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(pk);
            sig.update(body.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception exception) {
            return false;
        }
    }

    @Transactional
    void process(String body) {
        try {
            JsonNode root = json.readTree(body);
            String status = root.path("status").asText("");
            String reference = root.path("reference").asText("");
            String invoiceId = root.path("invoiceId").asText(null);

            if (!withinReplayWindow(root)) return;
            // Monobank can send two success webhooks per invoice: the first with
            // walletData.status="new" (no cardToken yet) and the second with
            // walletData.status="created" (cardToken populated). Including the
            // walletData.status in the event id lets both be processed so the
            // card token actually lands in the entitlement row.
            String walletStatus = root.path("walletData").path("status").asText("");
            String eventId = invoiceId == null
                    ? null
                    : invoiceId + ":" + status + (walletStatus.isBlank() ? "" : ":" + walletStatus);
            if (eventId != null && !idempotency.markProcessed("monobank", eventId)) return;

            if (reference.startsWith("renew-")) {
                handleRenewal(reference, status);
            } else {
                handleFirstPayment(root, reference, status, invoiceId);
            }
        } catch (Exception ex) {
            log.warn("Monobank webhook processing failed: {}", ex.getMessage());
        }
    }

    private void handleFirstPayment(JsonNode root, String reference, String status, String invoiceId) {
        String[] parts = reference.split(":");
        if (parts.length < 2) {
            log.warn("Monobank webhook bad reference: {}", reference);
            return;
        }
        String monoPlanId = parts[0];
        String userId = parts[1];
        var product = products.findAll().stream()
                .filter(p -> monoPlanId.equals(p.getMonobankPlanId()))
                .findFirst().orElse(null);
        if (product == null) {
            log.warn("Monobank webhook: no product for monoPlanId={}", monoPlanId);
            return;
        }
        EntitlementState state = mapState(status);
        if (state == null) {
            log.info("Monobank status={} ignored", status);
            return;
        }
        UserEntitlement entitlement = invoiceId == null
                ? newEntitlement(userId, product.getId(), null)
                : entitlements.findByOriginalTransactionId(invoiceId)
                        .orElseGet(() -> newEntitlement(userId, product.getId(), invoiceId));
        entitlement.setState(state);
        if (state == EntitlementState.ACTIVE) {
            Instant expires = Instant.now().plus(PERIOD);
            entitlement.setExpiresAt(expires);
            entitlement.setNextRenewalAt(expires.minus(RENEW_LEAD));
            entitlement.setRenewalRetryCount(0);
            JsonNode walletData = root.path("walletData");
            String walletId = walletData.path("walletId").asText(null);
            String cardToken = walletData.path("cardToken").asText(null);
            if (walletId != null && !walletId.isBlank()) entitlement.setMonobankWalletId(walletId);
            if (cardToken != null && !cardToken.isBlank()) entitlement.setMonobankCardToken(cardToken);
        }
        entitlements.save(entitlement);
    }

    private void handleRenewal(String reference, String status) {
        // reference = "renew-{userId}-{yyyyMM}"
        // userId is a UUID (contains dashes); split off the trailing -yyyyMM.
        String tail = reference.substring("renew-".length());
        int lastDash = tail.lastIndexOf('-');
        if (lastDash < 0) {
            log.warn("Monobank renewal webhook bad reference: {}", reference);
            return;
        }
        String userId = tail.substring(0, lastDash);
        var ent = entitlements.findActiveByUserId(userId).orElse(null);
        if (ent == null || ent.getSource() != EntitlementSource.MONOBANK) {
            log.warn("Monobank renewal webhook: no MONOBANK entitlement for userId={}", userId);
            return;
        }
        int maxRetries = props.monobank().recurring() != null
                ? props.monobank().recurring().graceMaxRetries() : 3;

        EntitlementState mapped = mapState(status);
        if (mapped == EntitlementState.ACTIVE) {
            Instant base = ent.getExpiresAt() != null && ent.getExpiresAt().isAfter(Instant.now())
                    ? ent.getExpiresAt()
                    : Instant.now();
            Instant expires = base.plus(PERIOD);
            ent.setExpiresAt(expires);
            ent.setNextRenewalAt(expires.minus(RENEW_LEAD));
            ent.setRenewalRetryCount(0);
            entitlements.save(ent);
        } else if (mapped == EntitlementState.EXPIRED) {
            int retries = ent.getRenewalRetryCount() + 1;
            ent.setRenewalRetryCount(retries);
            if (retries < maxRetries) {
                ent.setNextRenewalAt(Instant.now().plus(Duration.ofDays(1)));
            } else {
                ent.setNextRenewalAt(null);
            }
            entitlements.save(ent);
        }
    }

    private boolean withinReplayWindow(JsonNode root) {
        String modifiedDate = root.path("modifiedDate").asText(null);
        if (modifiedDate == null || modifiedDate.isBlank()) return true;
        try {
            Instant ts = Instant.parse(modifiedDate);
            Instant now = Instant.now();
            if (ts.isBefore(now.minusSeconds(600)) || ts.isAfter(now.plusSeconds(600))) {
                log.warn("Monobank webhook stale/future modifiedDate={} (now={}); ignoring", ts, now);
                return false;
            }
        } catch (DateTimeParseException dateTimeParseException) {
            log.warn("Monobank webhook unparseable modifiedDate={}; processing anyway", modifiedDate);
        }
        return true;
    }

    private static EntitlementState mapState(String status) {
        return switch (status) {
            case "success", "hold" -> EntitlementState.ACTIVE;
            case "expired", "reversed", "failure" -> EntitlementState.EXPIRED;
            default -> null;
        };
    }

    private UserEntitlement newEntitlement(String userId, String productId, String invoiceId) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(UUID.randomUUID().toString());
        entitlement.setUserId(userId);
        entitlement.setProductId(productId);
        entitlement.setOriginalTransactionId(invoiceId);
        entitlement.setSource(EntitlementSource.MONOBANK);
        return entitlement;
    }
}
