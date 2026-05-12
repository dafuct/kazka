package com.kazka.auth.token;

import com.kazka.auth.KazkaUserDetails;
import com.kazka.user.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class BearerTokenAuthenticationWebFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenIssuer tokenIssuer;
    private final UserRepository users;

    public BearerTokenAuthenticationWebFilter(TokenIssuer tokenIssuer,
                                              UserRepository users) {
        this.tokenIssuer = tokenIssuer;
        this.users = users;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return chain.filter(exchange);
        }

        return Mono.fromCallable(() -> tokenIssuer.verifyAccessToken(token))
                .onErrorResume(TokenIssuer.InvalidTokenException.class, ex -> Mono.empty())
                .flatMap(claims -> Mono.fromCallable(() -> users.findById(claims.userId()).orElse(null))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(user -> {
                    var principal = new KazkaUserDetails(user);
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    var context = new SecurityContextImpl(auth);
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))
                            .thenReturn(Boolean.TRUE);
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(Boolean.TRUE)))
                .then();
    }
}
