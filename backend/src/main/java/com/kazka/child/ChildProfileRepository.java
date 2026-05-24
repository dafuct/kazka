package com.kazka.child;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChildProfileRepository extends JpaRepository<ChildProfile, String> {
    List<ChildProfile> findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(String userId);
    List<ChildProfile> findByUserIdOrderByCreatedAtAsc(String userId);
    Optional<ChildProfile> findByIdAndUserId(String id, String userId);
    long countByUserIdAndArchivedAtIsNull(String userId);
}
