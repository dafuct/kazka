package com.kazka.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, String> {
    List<UserEntitlement> findByUserId(String userId);
    Optional<UserEntitlement> findByOriginalTransactionId(String originalTransactionId);
    List<EntitlementSummary> findSummariesByUserId(String userId);

    @Query("SELECT e FROM UserEntitlement e WHERE e.userId = :userId AND e.state IN ('ACTIVE', 'GRACE') ORDER BY e.expiresAt DESC")
    Optional<UserEntitlement> findActiveByUserId(@Param("userId") String userId);

    /** Projection used by the hot isPro() path — avoids loading LONGTEXT latest_jws. */
    interface EntitlementSummary {
        EntitlementState getState();
        Instant getExpiresAt();
    }
}
