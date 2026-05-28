package com.kazka.billing;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.billing.dto.CheckoutSessionRequest;
import com.kazka.billing.dto.CheckoutSessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/billing")
public class CheckoutSessionController {

    private final CheckoutSessionService service;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping(path = "/checkout-session", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CheckoutSessionResponse> create(@Valid @RequestBody CheckoutSessionRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> service.create(cu.userId(), req));
    }
}
