package com.kazka.billing.gift;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.billing.gift.dto.RedemptionResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class GiftCodeService {

    private final GiftCodeRepository codes;
    private final UserEntitlementRepository entitlements;

    @Transactional
    public RedemptionResultDto redeem(String rawCode, CurrentUser cu) {
        String normalized = normalize(rawCode);
        GiftCode giftCode = codes.findByCodeForUpdate(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (giftCode.getStatus() != GiftCodeStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.GONE, giftCode.getStatus().name().toLowerCase());
        }
        if (giftCode.getExpiresAt() != null && giftCode.getExpiresAt().isBefore(Instant.now())) {
            giftCode.setStatus(GiftCodeStatus.EXPIRED);
            codes.save(giftCode);
            throw new ResponseStatusException(HttpStatus.GONE, "expired");
        }

        giftCode.setStatus(GiftCodeStatus.REDEEMED);
        giftCode.setRedeemedBy(cu.userId());
        giftCode.setRedeemedAt(Instant.now());
        codes.save(giftCode);

        Instant now = Instant.now();
        Instant newExpiry = entitlements.findActiveByUserId(cu.userId())
                .map(existing -> extendExpiry(existing, giftCode.getDurationDays()))
                .orElseGet(() -> createEntitlement(cu.userId(), giftCode.getDurationDays(), now));

        return new RedemptionResultDto(newExpiry, giftCode.getDurationDays());
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
        UserEntitlement userEntitlement = new UserEntitlement();
        userEntitlement.setId(UUID.randomUUID().toString());
        userEntitlement.setUserId(userId);
        userEntitlement.setState(EntitlementState.ACTIVE);
        userEntitlement.setSource(EntitlementSource.GIFT);
        userEntitlement.setExpiresAt(expiresAt);
        entitlements.save(userEntitlement);
        return expiresAt;
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
