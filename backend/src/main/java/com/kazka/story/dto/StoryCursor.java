package com.kazka.story.dto;

import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Opaque cursor encoding (createdAt instant + story id). Encoded as
 * base64url of "{epochMillis}:{id}" — fits in a URL query param.
 */
public record StoryCursor(Instant createdAt, String id) {

    public String encode() {
        String raw = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    public static StoryCursor decode(String encoded) {
        Objects.requireNonNull(encoded, "encoded");
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded));
            int sep = raw.indexOf(':');
            if (sep < 1) throw new IllegalArgumentException("Invalid cursor format");
            return new StoryCursor(
                    Instant.ofEpochMilli(Long.parseLong(raw.substring(0, sep))),
                    raw.substring(sep + 1));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid cursor: " + encoded, e);
        }
    }
}
