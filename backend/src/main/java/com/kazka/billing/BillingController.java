package com.kazka.billing;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.billing.dto.EntitlementDto;
import com.kazka.billing.dto.IapVerifyRequest;
import com.kazka.billing.dto.ProductDto;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(BillingController.class);

    private final BillingService service;
    private final CurrentUserResolver currentUserResolver;

    public BillingController(BillingService service, CurrentUserResolver currentUserResolver) {
        this.service = service;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/products")
    public Mono<List<ProductDto>> products() {
        return service.listProducts()
                .map(list -> list.stream().map(ProductDto::from).toList());
    }

    @GetMapping("/entitlements")
    public Mono<List<EntitlementDto>> entitlements() {
        return currentUserResolver.requireUser()
                .flatMap(cu -> entitlementsFor(cu.userId()));
    }

    @PostMapping(path = "/iap/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<EntitlementDto>> verify(@Valid @RequestBody IapVerifyRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> service.verifyAndPersist(cu.userId(), req.signedTransaction())
                        .then(entitlementsFor(cu.userId())));
    }

    /** Apple ASN V2 webhook. Body is { "signedPayload": "<jws>" }.
     *  We swallow VerificationException so Apple doesn't retry a permanently bad payload. */
    @PostMapping(path = "/iap/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Void> webhook(@RequestBody Map<String, String> body) {
        String signed = body.get("signedPayload");
        if (signed == null || signed.isBlank()) {
            return Mono.error(new IllegalArgumentException("missing signedPayload"));
        }
        return service.ingestWebhook(signed)
                .onErrorResume(com.apple.itunes.storekit.verification.VerificationException.class, e -> {
                    log.warn("ASN V2 webhook: invalid signature; returning 200 to suppress retries: {}",
                            e.getMessage());
                    return Mono.empty();
                });
    }

    @PostMapping("/subscription/cancel")
    public Mono<List<EntitlementDto>> cancelSubscription() {
        return currentUserResolver.requireUser()
                .flatMap(cu -> service.revokeActiveForUser(cu.userId())
                        .then(entitlementsFor(cu.userId())));
    }

    private Mono<List<EntitlementDto>> entitlementsFor(String userId) {
        return service.findActive(userId)
                .flatMap(active -> Flux.fromIterable(active)
                        .flatMap(e -> service.findProductById(e.getProductId())
                                .map(p -> EntitlementDto.from(e, p)))
                        .collectList());
    }
}
