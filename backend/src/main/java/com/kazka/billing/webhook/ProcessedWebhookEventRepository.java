package com.kazka.billing.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository
        extends JpaRepository<ProcessedWebhookEvent, String> {

    boolean existsByProviderAndEventId(String provider, String eventId);
}
