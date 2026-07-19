package com.kazka.story;

/**
 * Lifecycle of a story's cached read-aloud narration.
 * NONE = never requested; GENERATING = synthesis in flight (atomic claim held);
 * READY = audio cached, key set; FAILED = synthesis errored (frontend shows an error state,
 * no Web Speech fallback).
 */
public enum NarrationStatus {
    NONE, GENERATING, READY, FAILED
}
