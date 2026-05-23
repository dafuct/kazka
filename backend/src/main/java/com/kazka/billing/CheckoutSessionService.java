package com.kazka.billing;

import com.kazka.billing.dto.CheckoutSessionRequest;
import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.billing.liqpay.LiqPayClient;
import com.kazka.billing.monobank.MonobankClient;
import com.kazka.billing.paddle.PaddleClient;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class CheckoutSessionService {

    private final SubscriptionProductRepository products;
    private final UserRepository users;
    private final PaddleClient paddle;
    private final LiqPayClient liqpay;
    private final MonobankClient monobank;

    public CheckoutSessionService(SubscriptionProductRepository products,
                                  UserRepository users,
                                  PaddleClient paddle,
                                  LiqPayClient liqpay,
                                  MonobankClient monobank) {
        this.products = products;
        this.users = users;
        this.paddle = paddle;
        this.liqpay = liqpay;
        this.monobank = monobank;
    }

    public Mono<CheckoutSessionResponse> create(String userId, CheckoutSessionRequest req) {
        return Mono.fromCallable(() -> {
            SubscriptionProduct product = products.findById(req.planId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown planId: " + req.planId()));
            User user = users.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
            return new Ctx(product, user);
        }).subscribeOn(Schedulers.boundedElastic())
          .flatMap(ctx -> switch (req.provider()) {
              case "paddle" -> paddle.createTransaction(
                      requireNotBlank(ctx.product().getPaddleProductId(), "paddleProductId"),
                      ctx.user().getId(), ctx.user().getEmail())
                  .map(pt -> new CheckoutSessionResponse("paddle", pt.checkoutUrl(), pt.id()));
              case "liqpay" -> liqpay.createCheckoutUrl(
                      requireNotBlank(ctx.product().getLiqpayPlanId(), "liqpayPlanId"),
                      ctx.user().getId(), ctx.product().getPriceMicro(), ctx.product().getCurrency())
                  .map(url -> new CheckoutSessionResponse("liqpay", url, null));
              case "monobank" -> monobank.createInvoiceUrl(
                      requireNotBlank(ctx.product().getMonobankPlanId(), "monobankPlanId"),
                      ctx.user().getId(), ctx.product().getPriceMicro(), ctx.product().getCurrency())
                  .map(url -> new CheckoutSessionResponse("monobank", url, null));
              default -> Mono.<CheckoutSessionResponse>error(new IllegalArgumentException(
                      "Unknown provider: " + req.provider()));
          });
    }

    private static String requireNotBlank(String s, String name) {
        if (s == null || s.isBlank()) throw new IllegalStateException(name + " not configured for this product");
        return s;
    }

    private record Ctx(SubscriptionProduct product, User user) {}
}
