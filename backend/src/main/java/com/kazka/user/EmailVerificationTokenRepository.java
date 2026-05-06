package com.kazka.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, String> {

    @Modifying
    @Query("update EmailVerificationToken t set t.consumedAt = :now " +
           "where t.userId = :userId and t.consumedAt is null")
    int consumeAllByUserId(@Param("userId") String userId, @Param("now") Instant now);
}
