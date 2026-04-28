package com.kazka.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class StaticUploadsConfig implements WebFluxConfigurer {

    private final UploadsProperties uploadsProperties;

    public StaticUploadsConfig(UploadsProperties uploadsProperties) {
        this.uploadsProperties = uploadsProperties;
    }

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
