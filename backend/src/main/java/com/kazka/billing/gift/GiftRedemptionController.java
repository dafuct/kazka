package com.kazka.billing.gift;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.billing.gift.dto.RedeemGiftRequest;
import com.kazka.billing.gift.dto.RedemptionResultDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class GiftRedemptionController {

    private final GiftCodeService svc;
    private final CurrentUserResolver currentUserResolver;

    public GiftRedemptionController(GiftCodeService svc, CurrentUserResolver currentUserResolver) {
        this.svc = svc;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/billing/gift/redeem")
    public Mono<RedemptionResultDto> redeem(@Valid @RequestBody RedeemGiftRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromCallable(() -> svc.redeem(req.code(), cu))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
