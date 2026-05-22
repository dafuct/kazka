package com.kazka.billing.paddle;

import reactor.core.publisher.Mono;

public interface PaddleClient {
    Mono<String> createTransaction(String paddleProductId, String userId, String userEmail);
}
