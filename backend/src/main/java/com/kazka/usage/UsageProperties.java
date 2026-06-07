package com.kazka.usage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kazka.usage")
public record UsageProperties(Integer monthlyTaleLimit) {
    public int limitOrDefault() {
        return monthlyTaleLimit == null ? 30 : monthlyTaleLimit;
    }
}
