package com.kazka.billing;

import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Component
public class EntitlementResolver {

    private final UserEntitlementRepository entitlements;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public boolean isPro(String userId) {
        // Admins implicitly have Pro access across every gate that calls isPro().
        if (users.findById(userId).map(u -> u.getRole() == UserRole.ADMIN).orElse(false)) {
            return true;
        }
        return entitlements.findSummariesByUserId(userId).stream().anyMatch(this::isActiveOrInGrace);
    }

    private boolean isActiveOrInGrace(UserEntitlementRepository.EntitlementSummary e) {
        if (e.getState() == EntitlementState.GRACE) return true;
        if (e.getState() != EntitlementState.ACTIVE) return false;
        return e.getExpiresAt() == null || e.getExpiresAt().isAfter(Instant.now());
    }
}
