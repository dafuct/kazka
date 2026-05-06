package com.kazka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Explicit Jackson 2.x ObjectMapper bean — Spring Boot 4 ships Jackson 3.x
     * (tools.jackson) and does not auto-configure com.fasterxml.jackson.databind.ObjectMapper.
     * SecurityConfig and auth handlers depend on this bean for JSON serialisation.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
