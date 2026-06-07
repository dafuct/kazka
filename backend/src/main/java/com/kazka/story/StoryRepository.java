package com.kazka.story;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, String> {
    Page<Story> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Story> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<Story> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);

    long countByUserIdAndCreatedAtAfter(String userId, Instant since);

    long countByChildProfileId(String childProfileId);

    List<Story> findTop5ByUserIdOrderByCreatedAtDesc(String userId);

    List<Story> findAllByShowcaseTrueOrderByCreatedAtDesc();

    Page<Story> findAllByUserIdAndChildProfileIdOrderByCreatedAtDesc(
            String userId, String childProfileId, Pageable pageable);

    Page<Story> findAllByUserIdAndChildProfileIdIsNullOrderByCreatedAtDesc(
            String userId, Pageable pageable);

    Optional<Story> findFirstByChildProfileIdOrderByCreatedAtDesc(String childProfileId);

    @Query("SELECT s FROM Story s WHERE s.userId = :userId " +
           "AND (:createdAt IS NULL OR (s.createdAt < :createdAt OR (s.createdAt = :createdAt AND s.id < :id))) " +
           "ORDER BY s.createdAt DESC, s.id DESC")
    List<Story> findByCursor(
            @Param("userId") String userId,
            @Param("createdAt") Instant createdAt,
            @Param("id") String id,
            Pageable pageable);
}
