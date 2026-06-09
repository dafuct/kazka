package com.kazka.narration;

/**
 * Narration status returned to the client. {@code url} is the presigned/local audio URL when
 * {@code status == "READY"}, otherwise null.
 */
public record NarrationResponse(String status, String url) {}
