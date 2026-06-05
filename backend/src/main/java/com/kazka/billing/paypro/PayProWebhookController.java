package com.kazka.billing.paypro;

import com.kazka.billing.BillingProperties;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.SubscriptionProduct;
import com.kazka.billing.SubscriptionProductRepository;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.billing.webhook.WebhookIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/billing/webhook")
public class PayProWebhookController {

    private final PayProSignatureVerifier verifier;
    private final BillingProperties props;
    private final SubscriptionProductRepository products;
    private final UserEntitlementRepository entitlements;
    private final WebhookIdempotencyService idempotency;

    @PostMapping(path = "/paypro", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> handle(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            Map<String, String> form = toSingleValueMap(formData);
            String secret = props.paypro() == null ? null : props.paypro().ipnSecret();
            if (secret == null || secret.isBlank() || !verifier.verify(form, secret)) {
                log.warn("PayPro IPN signature failed; ignoring type={}", form.get("IPN_TYPE_NAME"));
                return Mono.empty();
            }
            return Mono.fromRunnable(() -> process(form))
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
        });
    }

    private static Map<String, String> toSingleValueMap(MultiValueMap<String, String> multi) {
        Map<String, String> out = new HashMap<>();
        multi.forEach((k, vals) -> { if (!vals.isEmpty()) out.put(k, vals.get(0)); });
        return out;
    }

    @Transactional
    void process(Map<String, String> ipn) {
        try {
            String typeName = ipn.getOrDefault("IPN_TYPE_NAME", "");
            String typeId = ipn.getOrDefault("IPN_TYPE_ID", "");
            String orderId = ipn.get("ORDER_ID");
            String subscriptionId = ipn.get("SUBSCRIPTION_ID");
            if (orderId == null || orderId.isBlank()) {
                // Without ORDER_ID we have no idempotency key — drop rather than collapse all
                // malformed events into a shared "null:<typeId>" bucket that silently dedupes.
                log.warn("PayPro IPN missing ORDER_ID; ignoring type={}", typeName);
                return;
            }
            String idempotencyKey = orderId + ":" + typeId;
            if (!idempotency.markProcessed("paypro", idempotencyKey)) {
                log.info("PayPro IPN duplicate; skipping type={} key={}", typeName, idempotencyKey);
                return;
            }

            EntitlementState state = mapState(typeId);
            if (state == null) {
                log.info("PayPro IPN {} (id={}) ignored — no state mapping", typeName, typeId);
                return;
            }

            String payproProductId = ipn.get("PRODUCT_ID");
            SubscriptionProduct product = payproProductId == null
                    ? null
                    : products.findByPayproProductId(payproProductId).orElse(null);
            if (product == null) {
                log.warn("PayPro IPN: no product for payproProductId={}", payproProductId);
                return;
            }

            Map<String, String> customFields = parseCustomFields(ipn.get("ORDER_CUSTOM_FIELDS"));
            String userId = customFields.get("x-kazka_user_id");
            if (userId == null || subscriptionId == null) {
                log.warn("PayPro IPN missing user or subscription id: type={}", typeName);
                return;
            }

            UserEntitlement e = entitlements.findByOriginalTransactionId(subscriptionId).orElseGet(() -> {
                UserEntitlement n = new UserEntitlement();
                n.setId(UUID.randomUUID().toString());
                n.setUserId(userId);
                n.setProductId(product.getId());
                n.setOriginalTransactionId(subscriptionId);
                n.setSource(EntitlementSource.PAYPRO);
                return n;
            });
            e.setState(state);
            Instant expires = parseExpires(ipn.get("SUBSCRIPTION_NEXT_CHARGE_DATE"));
            if (expires != null) e.setExpiresAt(expires);
            else if (state == EntitlementState.REVOKED || state == EntitlementState.EXPIRED) {
                e.setExpiresAt(Instant.now());
            }
            entitlements.save(e);
            log.info("PayPro IPN processed: type={} sub={} userId={} state={}", typeName, subscriptionId, userId, state);
        } catch (Exception ex) {
            log.warn("PayPro IPN processing failed: {}", ex.getMessage(), ex);
        }
    }

    private static EntitlementState mapState(String typeId) {
        return switch (Optional.ofNullable(typeId).orElse("")) {
            case "1", "6", "9", "13" -> EntitlementState.ACTIVE;   // OrderCharged, ChargeSucceed, Renewed, TrialCharge
            case "7" -> EntitlementState.GRACE;                     // ChargeFailed
            case "8", "11" -> EntitlementState.EXPIRED;             // Suspended, Finished
            case "10" -> EntitlementState.REVOKED;                  // Terminated
            default -> null;
        };
    }

    private static Map<String, String> parseCustomFields(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isBlank()) return out;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }

    private static Instant parseExpires(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Instant.parse(s); }
        catch (DateTimeParseException e) { return null; }
    }
}
