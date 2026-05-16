package com.kazka.auth.google;

import com.kazka.auth.AuthProperties;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenVerifier {

    private final AuthProperties.Google google;

    public GoogleIdTokenVerifier(AuthProperties.Google google) {
        this.google = google;
    }

    public Verified verify(String idToken) {
        throw new InvalidGoogleTokenException("not implemented");
    }

    public record Verified(String subject, String email, String name) {}

    public static final class InvalidGoogleTokenException extends RuntimeException {
        public InvalidGoogleTokenException(String message) { super(message); }
    }
}
