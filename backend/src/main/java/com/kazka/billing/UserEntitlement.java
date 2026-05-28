package com.kazka.billing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_entitlements")
@Getter
@Setter
public class UserEntitlement {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "product_id", nullable = true, length = 36)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EntitlementState state;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "latest_jws", columnDefinition = "LONGTEXT")
    private String latestJws;

    @Column(name = "original_transaction_id", length = 64)
    private String originalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private EntitlementSource source = EntitlementSource.APPLE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;
}
