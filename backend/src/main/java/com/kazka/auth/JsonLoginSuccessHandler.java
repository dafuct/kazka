package com.kazka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.user.UserDto;
import com.kazka.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class JsonLoginSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ObjectMapper mapper;
    private UserRepository users;

    JsonLoginSuccessHandler(ObjectMapper mapper) { this.mapper = mapper; }

    void setUsers(UserRepository users) { this.users = users; }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication auth) {
        KazkaUserDetails kud = (KazkaUserDetails) auth.getPrincipal();
        return Mono.fromCallable(() -> users.findById(kud.getUserId()).orElseThrow())
                .subscribeOn(Schedulers.boundedElastic())
                .map(UserDto::from)
                .flatMap(dto -> writeBody(exchange, dto));
    }

    private Mono<Void> writeBody(WebFilterExchange exchange, UserDto dto) {
        var resp = exchange.getExchange().getResponse();
        resp.setStatusCode(HttpStatus.OK);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = mapper.writeValueAsBytes(java.util.Map.of("user", dto));
            return resp.writeWith(Mono.just(resp.bufferFactory().wrap(body)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
