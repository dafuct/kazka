package com.kazka.illustration;

import com.kazka.story.Theme;

/**
 * Persists story illustration PNGs and resolves their browser URLs. Two implementations:
 * {@link FilesystemImageStorage} (dev/test) and {@code R2ImageStorage} (production, presigned URLs).
 */
public interface ImageStorage extends ImageUrlResolver {

    /** Stores a PNG for the given story + theme and returns the bare storage key. */
    String store(String storyId, Theme theme, byte[] png);

    /** Removes every illustration variant for a story (best-effort). */
    void delete(String storyId);
}
