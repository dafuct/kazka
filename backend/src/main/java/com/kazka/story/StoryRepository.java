package com.kazka.story;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, String> {
    Page<Story> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Story> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<Story> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
}
