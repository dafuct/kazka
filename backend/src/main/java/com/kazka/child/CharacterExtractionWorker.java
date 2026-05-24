package com.kazka.child;

import org.springframework.stereotype.Component;

/**
 * No-op stub until Task 22 lands the real async worker.
 * Allows StoryService to compile and integrate the kickoff point now.
 */
@Component
public class CharacterExtractionWorker {
    public void enqueue(String storyId, String childProfileId, String userId) {
        // Real implementation in Task 22
    }
}
