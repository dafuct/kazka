package com.kazka.child;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, String> {
    List<Character> findByChildProfileIdAndArchivedAtIsNullOrderByLastUsedAtDescCreatedAtAsc(String childProfileId);
    Optional<Character> findByChildProfileIdAndName(String childProfileId, String name);
    long countByChildProfileIdAndArchivedAtIsNull(String childProfileId);
}
