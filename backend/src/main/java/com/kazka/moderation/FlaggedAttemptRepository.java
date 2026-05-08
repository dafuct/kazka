package com.kazka.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FlaggedAttemptRepository extends JpaRepository<FlaggedAttempt, String> {

    /**
     * Counts flagged attempts for a user within the trailing window that COUNT toward suspension.
     * JUDGE_UNAVAILABLE and IMAGE_SCENE rows do not count — the user did not author them.
     */
    @Query("""
            SELECT COUNT(f) FROM FlaggedAttempt f
            WHERE f.userId = :userId
              AND f.createdAt >= :since
              AND f.pipeline = com.kazka.moderation.ModerationPipeline.TEXT_INPUT
              AND f.category <> com.kazka.moderation.ModerationCategory.JUDGE_UNAVAILABLE
            """)
    long countCountableInWindow(@Param("userId") String userId, @Param("since") Instant since);

    List<FlaggedAttempt> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<FlaggedAttempt> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long deleteByCreatedAtBefore(Instant cutoff);
}
