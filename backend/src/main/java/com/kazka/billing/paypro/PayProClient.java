package com.kazka.billing.paypro;

import reactor.core.publisher.Mono;

public interface PayProClient {
    Mono<Void> terminate(String subscriptionId);
}
