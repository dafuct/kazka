package com.kazka.child;

import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class CharacterExtractionWorker {

    private final StoryRepository stories;
    private final CharacterExtractionService extraction;

    @Async
    @Transactional
    public CompletableFuture<Void> enqueueAsync(String storyId) {
        run(storyId);
        return CompletableFuture.completedFuture(null);
    }

    /** Convenience entry point called from StoryService at end-of-stream. Delegates to async path. */
    public void enqueue(String storyId, String childProfileId, String userId) {
        enqueueAsync(storyId);
    }

    /**
     * Runs the extraction LLM call and updates extraction_status accordingly.
     * Status flow: PENDING -> RUNNING -> DONE | FAILED | SKIPPED.
     * Race vs DELETE: returns cleanly when the story is gone (no exception).
     */
    public void run(String storyId) {
        Story story = stories.findById(storyId).orElse(null);
        if (story == null) {
            log.info("Extraction skipped: story {} not found (possible race vs DELETE)", storyId);
            return;
        }
        if (story.getChildProfileId() == null) {
            story.setExtractionStatus(ExtractionStatus.SKIPPED);
            stories.save(story);
            return;
        }
        if (story.getExtractionStatus() == ExtractionStatus.RUNNING) {
            return; // idempotency: another invocation is in flight
        }
        story.setExtractionStatus(ExtractionStatus.RUNNING);
        stories.save(story);
        try {
            extraction.extract(story.getContent()).block();
            // We don't persist candidates — confirmation happens via REST endpoint.
            // The candidate list is re-derived on demand via GET /api/stories/{id}/extraction-candidates.
            story.setExtractionStatus(ExtractionStatus.DONE);
            stories.save(story);
        } catch (Exception e) {
            log.warn("Extraction failed for story {}: {}", storyId, e.getMessage());
            story.setExtractionStatus(ExtractionStatus.FAILED);
            stories.save(story);
        }
    }
}
