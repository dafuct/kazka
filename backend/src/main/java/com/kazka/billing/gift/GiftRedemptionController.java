package com.kazka.billing.gift;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.billing.gift.dto.RedeemGiftRequest;
import com.kazka.billing.gift.dto.RedemptionResultDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@RestController
public class GiftRedemptionController {

    private final GiftCodeService svc;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/api/billing/gift/redeem")
    public Mono<RedemptionResultDto> redeem(@Valid @RequestBody RedeemGiftRequest req) {
        return currentUserResolver.requireUser()
                .flatMap(currentUser -> Mono.fromCallable(() -> svc.redeem(req.code(), currentUser))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
