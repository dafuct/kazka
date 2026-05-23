package com.kazka.billing.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "processed_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pwe_provider_event",
                columnNames = {"provider", "event_id"}))
public class ProcessedWebhookEvent {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "provider", nullable = false, length = 16)
    private String provider;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Instant getProcessedAt() { return processedAt; }
}
