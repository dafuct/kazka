package com.kazka.story;

/**
 * Lifecycle of a story's cached read-aloud narration.
 * NONE = never requested; GENERATING = synthesis in flight (atomic claim held);
 * READY = WAV cached, key set; FAILED = synthesis errored (frontend falls back to Web Speech).
 */
public enum NarrationStatus {
    NONE, GENERATING, READY, FAILED
}
