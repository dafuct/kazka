package com.kazka.child;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class ChildRateLimiter {

    // Must stay >= the max batch size (20, from CreateChildProfilesBatchRequest) so a single
    // full valid batch can't be rejected by an otherwise-empty 24h window.
    static final int DAILY_LIMIT = 50;
    static final Duration WINDOW = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public void assertAndIncrement(String userId) {
        assertAndIncrement(userId, 1);
    }

    /** Reserves {@code n} creations against the 24h window; throws if it would exceed the ceiling. */
    public void assertAndIncrement(String userId, int n) {
        String key = "child:create:" + userId;
        Long count = redis.opsForValue().increment(key, n);
        if (count != null && count == n) {
            redis.expire(key, WINDOW);
        }
        if (count != null && count > DAILY_LIMIT) {
            throw new TooManyChildCreatesException();
        }
    }

    public static class TooManyChildCreatesException extends RuntimeException {
        public TooManyChildCreatesException() {
            super("Too many child profile creations in the last 24h (limit " + DAILY_LIMIT + ")");
        }
    }
}
