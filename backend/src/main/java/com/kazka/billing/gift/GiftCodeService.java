package com.kazka.billing.gift;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.billing.gift.dto.RedemptionResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class GiftCodeService {

    private static final Logger log = LoggerFactory.getLogger(GiftCodeService.class);

    private final GiftCodeRepository codes;
    private final UserEntitlementRepository entitlements;

    public GiftCodeService(GiftCodeRepository codes, UserEntitlementRepository entitlements) {
        this.codes = codes;
        this.entitlements = entitlements;
    }

    @Transactional
    public RedemptionResultDto redeem(String rawCode, CurrentUser cu) {
        String normalized = normalize(rawCode);
        GiftCode g = codes.findByCodeForUpdate(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (g.getStatus() != GiftCodeStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.GONE, g.getStatus().name().toLowerCase());
        }
        if (g.getExpiresAt() != null && g.getExpiresAt().isBefore(Instant.now())) {
            g.setStatus(GiftCodeStatus.EXPIRED);
            codes.save(g);
            throw new ResponseStatusException(HttpStatus.GONE, "expired");
        }

        g.setStatus(GiftCodeStatus.REDEEMED);
        g.setRedeemedBy(cu.userId());
        g.setRedeemedAt(Instant.now());
        codes.save(g);

        Instant now = Instant.now();
        Instant newExpiry = entitlements.findActiveByUserId(cu.userId())
                .map(existing -> extendExpiry(existing, g.getDurationDays()))
                .orElseGet(() -> createEntitlement(cu.userId(), g.getDurationDays(), now));

        return new RedemptionResultDto(newExpiry, g.getDurationDays());
    }

    private Instant extendExpiry(UserEntitlement existing, int days) {
        Instant base = existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(Instant.now())
                ? existing.getExpiresAt()
                : Instant.now();
        Instant next = base.plus(days, ChronoUnit.DAYS);
        existing.setExpiresAt(next);
        existing.setState(EntitlementState.ACTIVE);
        entitlements.save(existing);
        return next;
    }

    private Instant createEntitlement(String userId, int days, Instant now) {
        Instant expiresAt = now.plus(days, ChronoUnit.DAYS);
        UserEntitlement e = new UserEntitlement();
        e.setId(UUID.randomUUID().toString());
        e.setUserId(userId);
        e.setProductId("gift");
        e.setState(EntitlementState.ACTIVE);
        e.setSource(EntitlementSource.GIFT);
        e.setExpiresAt(expiresAt);
        entitlements.save(e);
        return expiresAt;
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
