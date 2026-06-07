package com.kazka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JsonLoginConverter implements ServerAuthenticationConverter {

    private final ObjectMapper mapper;

    JsonLoginConverter(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    @NullMarked
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(buf -> {
                    try {
                        byte[] bytes = new byte[buf.readableByteCount()];
                        buf.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(buf);
                    }
                })
                .map(bytes -> {
                    try {
                        var node = mapper.readTree(bytes);
                        String email = node.path("email").asText("").trim().toLowerCase();
                        String password = node.path("password").asText("");
                        return (Authentication) new UsernamePasswordAuthenticationToken(email, password);
                    } catch (Exception exception) {
                        throw new IllegalArgumentException("invalid login body");
                    }
                });
    }
}
