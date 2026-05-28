package com.kazka.child;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class ChildRateLimiter {

    static final int DAILY_LIMIT = 5;
    static final Duration WINDOW = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public void assertAndIncrement(String userId) {
        String key = "child:create:" + userId;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
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
