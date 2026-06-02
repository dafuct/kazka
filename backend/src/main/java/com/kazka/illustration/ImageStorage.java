package com.kazka.illustration;

/**
 * Persists comics panel PNGs and resolves their browser URLs. Two implementations:
 * {@link FilesystemImageStorage} (dev/test) and {@code R2ImageStorage} (production, presigned URLs).
 *
 * Keys are derived from (storyId, panelIndex). Character-consistency conditioning between
 * panels happens in {@code ComicsBuilder} via in-memory byte arrays — storage never needs
 * to read bytes back.
 */
public interface ImageStorage extends ImageUrlResolver {

    /** Persist one panel image and return its storage key. */
    String storePanel(String storyId, int panelIndex, byte[] png);

    /** Delete one stored image by key (called from the comics retry / delete flows). */
    void deleteByKey(String key);
}
