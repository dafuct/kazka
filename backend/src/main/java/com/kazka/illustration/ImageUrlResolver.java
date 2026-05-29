package com.kazka.illustration;

/**
 * Resolves a stored image key (e.g. {@code "story-1-light.png"}) to a URL the browser can load.
 * The filesystem backend returns a local {@code /uploads/...} path; the R2 backend returns a
 * short-lived presigned URL. Returns {@code null} when the key is {@code null}.
 */
public interface ImageUrlResolver {
    String urlFor(String key);
}
