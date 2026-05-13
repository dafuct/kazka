package com.kazka.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "subscription_products")
public class SubscriptionProduct {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "apple_product_id", nullable = false, unique = true, length = 120)
    private String appleProductId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "price_micro", nullable = false)
    private long priceMicro;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false, length = 16)
    private String period;

    @Column(nullable = false, length = 32)
    private String tier;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAppleProductId() { return appleProductId; }
    public void setAppleProductId(String v) { this.appleProductId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public long getPriceMicro() { return priceMicro; }
    public void setPriceMicro(long v) { this.priceMicro = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getPeriod() { return period; }
    public void setPeriod(String v) { this.period = v; }
    public String getTier() { return tier; }
    public void setTier(String v) { this.tier = v; }
    public Instant getCreatedAt() { return createdAt; }
}
