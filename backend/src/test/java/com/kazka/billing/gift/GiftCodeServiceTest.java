package com.kazka.billing.gift;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.billing.EntitlementSource;
import com.kazka.billing.EntitlementState;
import com.kazka.billing.UserEntitlement;
import com.kazka.billing.UserEntitlementRepository;
import com.kazka.billing.gift.dto.RedemptionResultDto;
import com.kazka.user.UserRole;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GiftCodeServiceTest {

    @Mock GiftCodeRepository codes;
    @Mock UserEntitlementRepository entitlements;
    @InjectMocks GiftCodeService svc;

    private CurrentUser user() {
        return new CurrentUser("u1", UserRole.USER);
    }

    private GiftCode available(String code, int days) {
        GiftCode g = new GiftCode();
        g.setCode(code);
        g.setDurationDays(days);
        g.setStatus(GiftCodeStatus.AVAILABLE);
        return g;
    }

    @Test
    void redeem_404_when_code_not_found() {
        when(codes.findByCodeForUpdate("ABCD1234")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.redeem("abcd-1234", user()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void redeem_410_when_already_redeemed() {
        GiftCode g = available("X", 30);
        g.setStatus(GiftCodeStatus.REDEEMED);
        when(codes.findByCodeForUpdate("X")).thenReturn(Optional.of(g));

        assertThatThrownBy(() -> svc.redeem("X", user()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.GONE));
    }

    @Test
    void redeem_410_when_expired() {
        GiftCode g = available("X", 30);
        g.setExpiresAt(Instant.now().minusSeconds(60));
        when(codes.findByCodeForUpdate("X")).thenReturn(Optional.of(g));

        assertThatThrownBy(() -> svc.redeem("X", user()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.GONE));
    }

    @Test
    void redeem_happy_path_creates_new_entitlement() {
        GiftCode g = available("CODE1234", 30);
        when(codes.findByCodeForUpdate("CODE1234")).thenReturn(Optional.of(g));
        when(entitlements.findActiveByUserId("u1")).thenReturn(Optional.empty());
        when(entitlements.save(any(UserEntitlement.class))).thenAnswer(i -> i.getArgument(0));

        RedemptionResultDto result = svc.redeem("code-1234", user());

        assertThat(result.durationDays()).isEqualTo(30);
        assertThat(result.expiresAt()).isAfter(Instant.now());
        assertThat(g.getStatus()).isEqualTo(GiftCodeStatus.REDEEMED);
        assertThat(g.getRedeemedBy()).isEqualTo("u1");

        ArgumentCaptor<UserEntitlement> captor = ArgumentCaptor.forClass(UserEntitlement.class);
        verify(entitlements).save(captor.capture());
        UserEntitlement saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("u1");
        assertThat(saved.getState()).isEqualTo(EntitlementState.ACTIVE);
        assertThat(saved.getSource()).isEqualTo(EntitlementSource.GIFT);
    }

    @Test
    void redeem_extends_existing_active_entitlement() {
        GiftCode g = available("EXTEND", 30);
        when(codes.findByCodeForUpdate("EXTEND")).thenReturn(Optional.of(g));

        UserEntitlement existing = new UserEntitlement();
        existing.setId("e1");
        existing.setUserId("u1");
        existing.setState(EntitlementState.ACTIVE);
        existing.setSource(EntitlementSource.APPLE);
        Instant currentExpiry = Instant.now().plusSeconds(86400L * 10);
        existing.setExpiresAt(currentExpiry);

        when(entitlements.findActiveByUserId("u1")).thenReturn(Optional.of(existing));
        when(entitlements.save(any(UserEntitlement.class))).thenAnswer(i -> i.getArgument(0));

        RedemptionResultDto result = svc.redeem("EXTEND", user());

        Instant expected = currentExpiry.plus(30, ChronoUnit.DAYS);
        assertThat(result.expiresAt()).isCloseTo(expected, new TemporalUnitWithinOffset(2, ChronoUnit.SECONDS));
        assertThat(existing.getExpiresAt()).isEqualTo(result.expiresAt());
    }
}
