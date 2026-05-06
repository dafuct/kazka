package com.kazka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JsonLoginConverter implements ServerAuthenticationConverter {

    private final ObjectMapper mapper;

    JsonLoginConverter(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return exchange.getRequest().getBody()
                .reduce(new java.io.ByteArrayOutputStream(), (out, buf) -> {
                    try {
                        out.writeBytes(buf.asInputStream().readAllBytes());
                    } catch (java.io.IOException e) {
                        throw new IllegalStateException("failed to read request body", e);
                    }
                    return out;
                })
                .map(out -> {
                    try {
                        var node = mapper.readTree(out.toByteArray());
                        String email = node.path("email").asText("").trim().toLowerCase();
                        String password = node.path("password").asText("");
                        return (Authentication) new UsernamePasswordAuthenticationToken(email, password);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("invalid login body");
                    }
                });
    }
}
