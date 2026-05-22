package com.kazka.billing.monobank;

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

import java.util.UUID;

@RestController
@RequestMapping("/api/billing/webhook")
public class MonobankWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MonobankWebhookController.class);

    private final BillingProperties props;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;
    private final ObjectMapper json = new ObjectMapper();

    public MonobankWebhookController(BillingProperties props,
                                     SubscriptionProductRepository products,
                                     UserEntitlementRepository entitlements) {
        this.props = props;
        this.products = products;
        this.entitlements = entitlements;
    }

    @PostMapping(path = "/monobank", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> handle(@RequestHeader(name = "X-Sign", required = false) String sign,
                             @RequestBody String body) {
        String expected = props.monobank() == null ? null : props.monobank().webhookPublicKey();
        if (expected == null || expected.isBlank() || !expected.equals(sign)) {
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
            String invoiceId = root.path("invoiceId").asText(null);
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
            case "expired", "reversed" -> EntitlementState.EXPIRED;
            case "failure" -> EntitlementState.EXPIRED;
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
