package com.kazka.billing.paddle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.billing.BillingProperties;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing/webhook")
public class PaddleWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaddleWebhookController.class);

    private final PaddleSignatureVerifier verifier;
    private final BillingProperties props;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;
    private final ObjectMapper json = new ObjectMapper();

    public PaddleWebhookController(PaddleSignatureVerifier verifier,
                                   BillingProperties props,
                                   SubscriptionProductRepository products,
                                   UserEntitlementRepository entitlements) {
        this.verifier = verifier;
        this.props = props;
        this.products = products;
        this.entitlements = entitlements;
    }

    @PostMapping(path = "/paddle", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> handle(@RequestHeader(name = "Paddle-Signature", required = false) String sig,
                             @RequestBody String body) {
        String secret = props.paddle() == null ? null : props.paddle().webhookSecret();
        if (secret == null || secret.isBlank() || !verifier.verify(sig, body, secret)) {
            log.warn("Paddle webhook signature failed; ignoring");
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> process(body))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Transactional
    void process(String body) {
        try {
            JsonNode root = json.readTree(body);
            String eventType = root.path("event_type").asText("");
            JsonNode data = root.path("data");
            String userId = data.path("custom_data").path("kazka_user_id").asText(null);
            String paddleProductId = data.path("items").path(0).path("price").path("id").asText(null);
            if (userId == null || paddleProductId == null) {
                log.warn("Paddle webhook missing user or product id: type={}", eventType);
                return;
            }
            var product = products.findByPaddleProductId(paddleProductId).orElse(null);
            if (product == null) {
                log.warn("Paddle webhook: no product for paddleProductId={}", paddleProductId);
                return;
            }
            EntitlementState state = mapState(eventType);
            if (state == null) {
                log.info("Paddle event {} ignored (no state mapping)", eventType);
                return;
            }
            Instant expiresAt = parseExpires(data);
            String txnId = data.path("id").asText(null);
            UserEntitlement e = entitlements.findByOriginalTransactionId(txnId).orElseGet(() -> {
                UserEntitlement n = new UserEntitlement();
                n.setId(UUID.randomUUID().toString());
                n.setUserId(userId);
                n.setProductId(product.getId());
                n.setOriginalTransactionId(txnId);
                n.setSource(EntitlementSource.PADDLE);
                return n;
            });
            e.setState(state);
            e.setExpiresAt(expiresAt);
            entitlements.save(e);
        } catch (Exception ex) {
            log.warn("Paddle webhook processing failed: {}", ex.getMessage());
        }
    }

    private static EntitlementState mapState(String eventType) {
        return switch (eventType) {
            case "transaction.completed", "subscription.activated", "subscription.resumed" -> EntitlementState.ACTIVE;
            case "subscription.paused", "subscription.canceled" -> EntitlementState.EXPIRED;
            case "transaction.payment_failed" -> EntitlementState.GRACE;
            default -> null;
        };
    }

    private static Instant parseExpires(JsonNode data) {
        String next = data.path("next_billed_at").asText(null);
        if (next == null) return null;
        try { return Instant.parse(next); }
        catch (DateTimeParseException e) { return null; }
    }
}
