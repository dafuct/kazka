package com.kazka.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Component
public class EntitlementResolver {

    private final UserEntitlementRepository entitlements;

    @Transactional(readOnly = true)
    public boolean isPro(String userId) {
        return entitlements.findSummariesByUserId(userId).stream().anyMatch(this::isActiveOrInGrace);
    }

    private boolean isActiveOrInGrace(UserEntitlementRepository.EntitlementSummary e) {
        if (e.getState() == EntitlementState.GRACE) return true;
        if (e.getState() != EntitlementState.ACTIVE) return false;
        return e.getExpiresAt() == null || e.getExpiresAt().isAfter(Instant.now());
    }
}
