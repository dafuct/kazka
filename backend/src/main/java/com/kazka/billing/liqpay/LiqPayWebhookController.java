package com.kazka.billing.liqpay;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing/webhook")
public class LiqPayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LiqPayWebhookController.class);

    private final LiqPaySignatureVerifier verifier;
    private final BillingProperties props;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;
    private final ObjectMapper json = new ObjectMapper();

    public LiqPayWebhookController(LiqPaySignatureVerifier verifier,
                                   BillingProperties props,
                                   SubscriptionProductRepository products,
                                   UserEntitlementRepository entitlements) {
        this.verifier = verifier;
        this.props = props;
        this.products = products;
        this.entitlements = entitlements;
    }

    /** LiqPay POSTs application/x-www-form-urlencoded with data + signature. */
    @PostMapping(path = "/liqpay", consumes = "application/x-www-form-urlencoded")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> handle(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(form -> {
            String data = form.getFirst("data");
            String signature = form.getFirst("signature");
            String pk = props.liqpay() == null ? null : props.liqpay().privateKey();
            if (pk == null || pk.isBlank() || !verifier.verify(data, signature, pk)) {
                log.warn("LiqPay webhook signature failed; ignoring");
                return Mono.<Void>empty();
            }
            return Mono.fromRunnable(() -> process(data))
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
        });
    }

    @Transactional
    void process(String base64Data) {
        try {
            String body = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);
            JsonNode root = json.readTree(body);
            String status = root.path("status").asText("");
            String orderId = root.path("order_id").asText("");
            String[] parts = orderId.split(":");
            if (parts.length < 2) {
                log.warn("LiqPay webhook: order_id has wrong shape: {}", orderId);
                return;
            }
            String liqpayPlanId = parts[0];
            String userId = parts[1];
            var product = products.findAll().stream()
                    .filter(p -> liqpayPlanId.equals(p.getLiqpayPlanId()))
                    .findFirst().orElse(null);
            if (product == null) {
                log.warn("LiqPay webhook: no product for liqpayPlanId={}", liqpayPlanId);
                return;
            }
            EntitlementState state = mapState(status);
            if (state == null) {
                log.info("LiqPay status={} ignored", status);
                return;
            }
            String txnId = root.path("transaction_id").asText(null);
            UserEntitlement e = txnId == null
                    ? newEntitlement(userId, product.getId(), null)
                    : entitlements.findByOriginalTransactionId(txnId)
                          .orElseGet(() -> newEntitlement(userId, product.getId(), txnId));
            e.setState(state);
            String endDate = root.path("end_date").asText(null);
            if (endDate != null) {
                try { e.setExpiresAt(Instant.ofEpochMilli(Long.parseLong(endDate))); }
                catch (NumberFormatException ignore) {}
            }
            entitlements.save(e);
        } catch (Exception ex) {
            log.warn("LiqPay webhook processing failed: {}", ex.getMessage());
        }
    }

    private static EntitlementState mapState(String status) {
        return switch (status) {
            case "success", "subscribed", "wait_compensation" -> EntitlementState.ACTIVE;
            case "subscription_canceled", "subscription_expired" -> EntitlementState.EXPIRED;
            case "failure", "error" -> EntitlementState.EXPIRED;
            default -> null;
        };
    }

    private UserEntitlement newEntitlement(String userId, String productId, String txnId) {
        UserEntitlement e = new UserEntitlement();
        e.setId(UUID.randomUUID().toString());
        e.setUserId(userId);
        e.setProductId(productId);
        e.setOriginalTransactionId(txnId);
        e.setSource(EntitlementSource.LIQPAY);
        return e;
    }
}
