package com.kazka.auth.token;

import com.kazka.auth.AuthProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "kazka:refresh:";
    private static final SecureRandom RNG = new SecureRandom();

    private final ReactiveStringRedisTemplate redis;
    private final AuthProperties.Jwt jwt;

    public RefreshTokenService(ReactiveStringRedisTemplate redis,
                               AuthProperties authProperties) {
        this.redis = redis;
        this.jwt = authProperties.jwt();
    }

    public Mono<String> issue(String userId) {
        String token = generateToken();
        String key = KEY_PREFIX + token;
        return redis.opsForValue()
                .set(key, userId, jwt.refreshTtl())
                .thenReturn(token);
    }

    public Mono<String> resolveUserId(String token) {
        String key = KEY_PREFIX + token;
        return redis.opsForValue().get(key)
                .switchIfEmpty(Mono.error(new UnknownRefreshTokenException()));
    }

    public Mono<RotateResult> rotate(String oldToken) {
        String oldKey = KEY_PREFIX + oldToken;
        return redis.opsForValue().get(oldKey)
                .switchIfEmpty(Mono.error(new UnknownRefreshTokenException()))
                .flatMap(userId ->
                        redis.delete(oldKey)
                                .then(issue(userId))
                                .map(newToken -> new RotateResult(newToken, userId)));
    }

    public Mono<Void> revoke(String token) {
        return redis.delete(KEY_PREFIX + token).then();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotateResult(String newToken, String userId) {}

    public static final class UnknownRefreshTokenException extends RuntimeException {}
}
