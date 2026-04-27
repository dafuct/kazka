package com.kazka.ollama;

import com.kazka.config.OllamaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

@Component
public class OllamaModelInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OllamaModelInitializer.class);

    private final OllamaClient ollamaClient;
    private final OllamaProperties properties;

    public OllamaModelInitializer(OllamaClient ollamaClient, OllamaProperties properties) {
        this.ollamaClient = ollamaClient;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        pullAsync(properties.getTextModel());
        pullAsync(properties.getImageModel());
    }

    private void pullAsync(String model) {
        ollamaClient.pullModel(model)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(line -> log.info("Pull {}: {}", model, line))
                .doOnComplete(() -> log.info("Pull complete: {}", model))
                .doOnError(e -> log.warn("Pull error for {}: {}", model, e.getMessage()))
                .subscribe();
    }
}
