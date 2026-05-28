package com.kazka.billing;

import com.kazka.billing.dto.CheckoutSessionRequest;
import com.kazka.billing.dto.CheckoutSessionResponse;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutSessionService {

    private final SubscriptionProductRepository products;
    private final UserRepository users;
    private final Map<String, CheckoutProvider> providers;

    public CheckoutSessionService(SubscriptionProductRepository products,
                                  UserRepository users,
                                  List<CheckoutProvider> providers) {
        this.products = products;
        this.users = users;
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(CheckoutProvider::provider, Function.identity()));
    }

    public Mono<CheckoutSessionResponse> create(String userId, CheckoutSessionRequest req) {
        return Mono.fromCallable(() -> {
            SubscriptionProduct product = products.findById(req.planId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown planId: " + req.planId()));
            User user = users.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
            return new Ctx(product, user);
        }).subscribeOn(Schedulers.boundedElastic())
          .flatMap(ctx -> {
              CheckoutProvider provider = providers.get(req.provider());
              if (provider == null) {
                  return Mono.error(new IllegalArgumentException("Unknown provider: " + req.provider()));
              }
              return provider.createSession(ctx.user(), ctx.product());
          });
    }

    private record Ctx(SubscriptionProduct product, User user) {}
}
