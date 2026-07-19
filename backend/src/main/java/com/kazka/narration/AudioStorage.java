package com.kazka.narration;

/**
 * Persists cached narration WAVs and resolves their browser URLs. Mirrors
 * {@link com.kazka.illustration.ImageStorage}: {@link FilesystemAudioStorage} (dev/test) and
 * {@link R2AudioStorage} (prod, presigned URLs). One WAV per story (keyed by storyId).
 */
public interface AudioStorage extends AudioUrlResolver {

    /** Persist one story's narration audio and return its storage key. */
    String storeNarration(String storyId, byte[] bytes, String contentType, String fileExtension);

    /** Delete one stored narration by key. */
    void deleteByKey(String key);
}
