package com.kazka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HuggingFaceConfig {

    @Bean
    public WebClient textClient(WebClient.Builder builder, HuggingFaceProperties huggingFaceProperties) {
        return builder.clone()
                .baseUrl(huggingFaceProperties.getTextBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, getToken(huggingFaceProperties))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient imageClient(WebClient.Builder builder, HuggingFaceProperties huggingFaceProperties) {
        return builder.clone()
                .baseUrl(huggingFaceProperties.getImageBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, getToken(huggingFaceProperties))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    private String getToken(HuggingFaceProperties huggingFaceProperties) {
        return "Bearer " + huggingFaceProperties.getApiToken();
    }
}
