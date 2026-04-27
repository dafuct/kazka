package com.kazka.story;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, String> {
    Page<Story> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
