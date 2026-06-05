package com.kazka.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionProductRepository extends JpaRepository<SubscriptionProduct, String> {
    Optional<SubscriptionProduct> findByAppleProductId(String appleProductId);
    Optional<SubscriptionProduct> findByPayproProductId(String payproProductId);
}
