package com.kazka.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "subscription_products")
@Getter
@Setter
public class SubscriptionProduct {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "apple_product_id", nullable = false, length = 120)
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

    @Column(name = "monobank_plan_id", length = 120)
    private String monobankPlanId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;
}
