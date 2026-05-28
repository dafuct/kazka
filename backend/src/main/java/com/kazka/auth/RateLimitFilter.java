package com.kazka.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Fixed-window per-IP rate limiter for sensitive endpoints. Backed by Redis (already
 * provisioned for sessions), uses {@code INCR + EXPIRE} so the implementation is
 * cluster-safe without needing a separate dependency.
 *
 * <p>This is a defense in depth against credential brute force, email enumeration,
 * and bot-driven story generation. It is intentionally simple: when a bucket fills
 * it returns 429 until the window rolls over. There is no token-bucket smoothing —
 * the goal here is "slow attackers down enough to be noisy in logs," not perfect
 * traffic shaping.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "kazka.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter implements WebFilter {

    private static final String KEY_PREFIX = "kazka:ratelimit:";

    private final ReactiveStringRedisTemplate redis;
    private final List<Rule> rules = List.of(
            // Auth endpoints: small per-IP windows. Tight enough to make password
            // brute force / email enumeration impractical, loose enough not to
            // break legitimate retry loops.
            new Rule(HttpMethod.POST, "/api/auth/login",                    10, Duration.ofMinutes(1)),
            new Rule(HttpMethod.POST, "/api/auth/signup",                    5, Duration.ofMinutes(10)),
            new Rule(HttpMethod.POST, "/api/auth/password-reset/request",    5, Duration.ofMinutes(15)),
            new Rule(HttpMethod.POST, "/api/auth/password-reset/confirm",   10, Duration.ofMinutes(15)),
            new Rule(HttpMethod.POST, "/api/auth/token/login",              10, Duration.ofMinutes(1)),
            new Rule(HttpMethod.POST, "/api/auth/token/refresh",            30, Duration.ofMinutes(1)),
            new Rule(HttpMethod.POST, "/api/auth/oauth/apple",              10, Duration.ofMinutes(1)),
            new Rule(HttpMethod.POST, "/api/auth/oauth/google",             10, Duration.ofMinutes(1)),
            new Rule(HttpMethod.POST, "/api/auth/verify-email/resend",       3, Duration.ofMinutes(15)),
            // Expensive endpoint: limits per-IP burst at the platform edge before
            // FreeTierGate (which is per-user) and HuggingFace quota.
            new Rule(HttpMethod.POST, "/api/stories/generate",              20, Duration.ofMinutes(15))
    );

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        Rule rule = match(req);
        if (rule == null) return chain.filter(exchange);

        String ip = clientIp(req);
        String key = KEY_PREFIX + rule.path + ':' + ip;

        return redis.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> expirePromise = count == 1L
                            ? redis.expire(key, rule.window)
                            : Mono.just(true);
                    return expirePromise.thenReturn(count);
                })
                .flatMap(count -> {
                    if (count > rule.max) {
                        log.info("rate-limit hit ip={} path={} count={}", ip, rule.path, count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add(HttpHeaders.RETRY_AFTER,
                                Long.toString(rule.window.toSeconds()));
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                })
                // If Redis itself is down we fail open rather than block all traffic.
                // A flapping Redis would otherwise take the whole auth surface offline.
                .onErrorResume(err -> {
                    log.warn("rate-limit redis error path={} ip={}: {}", rule.path, ip, err.toString());
                    return chain.filter(exchange);
                });
    }

    private Rule match(ServerHttpRequest req) {
        for (Rule r : rules) {
            if (r.method.equals(req.getMethod()) && req.getPath().value().equals(r.path)) {
                return r;
            }
        }
        return null;
    }

    private static String clientIp(ServerHttpRequest req) {
        String fwd = req.getHeaders().getFirst("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            // First entry is the original client; the rest are intermediate proxies.
            int comma = fwd.indexOf(',');
            return (comma >= 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        return Optional.ofNullable(req.getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(Object::toString)
                .orElse("unknown");
    }

    private record Rule(HttpMethod method, String path, long max, Duration window) {}
}
