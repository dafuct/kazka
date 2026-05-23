package com.kazka.billing.paddle;

import reactor.core.publisher.Mono;

public interface PaddleClient {
    Mono<PaddleTransaction> createTransaction(String paddleProductId, String userId, String userEmail);
}
