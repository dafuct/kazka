package com.kazka.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
    Optional<User> findByAppleSubject(String appleSubject);
    boolean existsByEmail(String email);
    List<User> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> lockById(@Param("id") String id);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.storiesThisMonth = 0, u.counterResetAt = CURRENT_TIMESTAMP " +
            "WHERE u.counterResetAt IS NULL OR u.counterResetAt < :cutoff")
    int resetCountersUpdatedBefore(@Param("cutoff") Instant cutoff);
}
