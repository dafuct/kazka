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
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/billing/webhook")
public class MonobankWebhookController {

    private final BillingProperties props;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;
    private final WebhookIdempotencyService idempotency;
    private final ObjectMapper json = new ObjectMapper();

    @PostMapping(path = "/monobank", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> handle(@RequestHeader(name = "X-Sign", required = false) String sign,
                             @RequestBody String body) {
        String expected = props.monobank() == null ? null : props.monobank().webhookPublicKey();
        if (expected == null || expected.isBlank() || sign == null
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        sign.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Monobank webhook X-Sign mismatch; ignoring");
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
            String status = root.path("status").asText("");
            String reference = root.path("reference").asText("");
            // Replay window: modifiedDate is ISO 8601.
            String modifiedDate = root.path("modifiedDate").asText(null);
            if (modifiedDate != null && !modifiedDate.isBlank()) {
                try {
                    Instant ts = Instant.parse(modifiedDate);
                    Instant now = Instant.now();
                    if (ts.isBefore(now.minusSeconds(600)) || ts.isAfter(now.plusSeconds(600))) {
                        log.warn("Monobank webhook stale/future modifiedDate={} (now={}); ignoring", ts, now);
                        return;
                    }
                } catch (DateTimeParseException e) {
                    log.warn("Monobank webhook unparseable modifiedDate={}; processing anyway", modifiedDate);
                }
            }
            // Mono delivers invoiceId per transaction; combine with status so different
            // lifecycle events on the same invoice are not collapsed.
            String invoiceId = root.path("invoiceId").asText(null);
            String eventId = (invoiceId == null ? null : invoiceId + ":" + status);
            if (eventId != null && !idempotency.markProcessed("monobank", eventId)) {
                return;
            }
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
            UserEntitlement e = invoiceId == null
                    ? newEntitlement(userId, product.getId(), null)
                    : entitlements.findByOriginalTransactionId(invoiceId)
                            .orElseGet(() -> newEntitlement(userId, product.getId(), invoiceId));
            e.setState(state);
            entitlements.save(e);
        } catch (Exception ex) {
            log.warn("Monobank webhook processing failed: {}", ex.getMessage());
        }
    }

    private static EntitlementState mapState(String status) {
        return switch (status) {
            case "success", "hold" -> EntitlementState.ACTIVE;
            case "expired", "reversed", "failure" -> EntitlementState.EXPIRED;
            default -> null;
        };
    }

    private UserEntitlement newEntitlement(String userId, String productId, String invoiceId) {
        UserEntitlement e = new UserEntitlement();
        e.setId(UUID.randomUUID().toString());
        e.setUserId(userId);
        e.setProductId(productId);
        e.setOriginalTransactionId(invoiceId);
        e.setSource(EntitlementSource.MONOBANK);
        return e;
    }
}
