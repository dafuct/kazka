package com.kazka.billing;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_entitlements")
public class UserEntitlement {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "product_id", nullable = false, length = 36)
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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public EntitlementState getState() { return state; }
    public void setState(EntitlementState v) { this.state = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public String getLatestJws() { return latestJws; }
    public void setLatestJws(String v) { this.latestJws = v; }
    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String v) { this.originalTransactionId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
