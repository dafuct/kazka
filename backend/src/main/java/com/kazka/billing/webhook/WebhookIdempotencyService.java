package com.kazka.billing.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Records each inbound webhook event id so duplicate deliveries are processed at
 * most once. Apple ASN V2 retries on 5xx, Paddle retries on 5xx, LiqPay can
 * deliver the same event twice; without this gate a retry doubles entitlements
 * or re-applies a refund.
 *
 * <p>Insert-first semantics: callers should call {@link #markProcessed} BEFORE
 * mutating entitlement state. If the row already exists, the call returns
 * {@code false} and the caller must skip processing.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WebhookIdempotencyService {

    private final ProcessedWebhookEventRepository repo;

    /**
     * @return true if this is the first time we have seen ({@code provider}, {@code eventId});
     *         false if we have already processed it and the caller should skip.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(String provider, String eventId) {
        if (eventId == null || eventId.isBlank()) {
            // Without an event id we can't dedupe — be conservative and let the call through.
            // This is the correct policy when a provider's payload happens not to carry one.
            return true;
        }
        if (repo.existsByProviderAndEventId(provider, eventId)) {
            log.info("webhook duplicate ignored provider={} event_id={}", provider, eventId);
            return false;
        }
        try {
            ProcessedWebhookEvent e = new ProcessedWebhookEvent();
            e.setId(UUID.randomUUID().toString());
            e.setProvider(provider);
            e.setEventId(eventId);
            repo.save(e);
            return true;
        } catch (DataIntegrityViolationException race) {
            // Concurrent delivery of the same event — the other thread won.
            log.info("webhook duplicate raced provider={} event_id={}", provider, eventId);
            return false;
        }
    }
}
