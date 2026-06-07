package com.kazka.comics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StoryPanelRepository extends JpaRepository<StoryPanel, String> {

    /** Ordered list of panels for one story, ascending by panel_index. */
    List<StoryPanel> findByStoryIdOrderByPanelIndexAsc(String storyId);

    /** Used by list endpoints to fetch panels for many stories without N+1. */
    List<StoryPanel> findByStoryIdInOrderByStoryIdAscPanelIndexAsc(Collection<String> storyIds);

    /** Used by ComicsBuilder.deletePanels and the retry flow. */
    void deleteByStoryId(String storyId);

    /** Used by the /status endpoint to know how many panels exist for a story. */
    long countByStoryId(String storyId);

    /**
     * Used by the public showcase image route to verify the requested image key
     * actually belongs to the story before streaming it from disk.
     */
    boolean existsByStoryIdAndImagePath(String storyId, String imagePath);
}
