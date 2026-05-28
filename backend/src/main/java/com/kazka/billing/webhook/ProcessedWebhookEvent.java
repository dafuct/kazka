package com.kazka.billing.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "processed_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pwe_provider_event",
                columnNames = {"provider", "event_id"}))
@Getter
@Setter
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
    @Setter(AccessLevel.NONE)
    private Instant processedAt;
}
