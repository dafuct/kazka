package com.kazka.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, String> {
    List<UserEntitlement> findByUserId(String userId);
    Optional<UserEntitlement> findByOriginalTransactionId(String originalTransactionId);
}
