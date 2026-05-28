package com.kazka.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.session.data.redis.ReactiveRedisIndexedSessionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Component
public class SessionInvalidator {

    private final ReactiveRedisIndexedSessionRepository sessions;

    public void invalidateAllForUser(String userId) {
        sessions.findByPrincipalName(userId)
                .flatMapMany(map -> Flux.fromIterable(map.keySet()))
                .flatMap(sessions::deleteById)
                .blockLast();
    }
}
