package com.kazka.billing.gift;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "gift_codes")
@Getter
@Setter
public class GiftCode {
    @Id
    @Column(length = 20)
    private String code;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GiftCodeStatus status = GiftCodeStatus.AVAILABLE;

    @Column(name = "redeemed_by", length = 36)
    private String redeemedBy;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;
}
