package com.kazka.narration;

/** Resolves a stored narration key to a browser-playable URL (local /uploads or R2 presigned). */
public interface AudioUrlResolver {
    String urlFor(String key);
}
