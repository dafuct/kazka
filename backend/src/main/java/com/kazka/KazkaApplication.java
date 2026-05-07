package com.kazka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.kazka")
public class KazkaApplication {
    static void main(String[] args) {
        SpringApplication.run(KazkaApplication.class, args);
    }
}
