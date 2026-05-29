package com.kazka.config;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(name = "kazka.storage.provider", havingValue = "filesystem", matchIfMissing = true)
public class StaticUploadsConfig implements WebFluxConfigurer {

    private final UploadsProperties uploadsProperties;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String uploadsDir = uploadsProperties.getDir();
        if (!uploadsDir.endsWith("/")) {
            uploadsDir += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsDir);
    }
}
