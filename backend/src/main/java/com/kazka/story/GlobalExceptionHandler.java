package com.kazka.story;

import com.kazka.auth.apple.AppleIdentityTokenVerifier;
import com.kazka.auth.exception.EmailAlreadyExistsException;
import com.kazka.auth.exception.EmailNotVerifiedException;
import com.kazka.auth.exception.InvalidCredentialsException;
import com.kazka.auth.exception.InvalidRefreshTokenException;
import com.kazka.auth.exception.InvalidTokenException;
import com.kazka.auth.exception.MailDeliveryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", codeFor(ex));
        if (ex.getReason() != null) body.put("message", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefresh(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_REFRESH_TOKEN"));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailTaken(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "EMAIL_TAKEN"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "EMAIL_NOT_VERIFIED"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "TOKEN_INVALID"));
    }

    @ExceptionHandler(AppleIdentityTokenVerifier.InvalidAppleTokenException.class)
    public ResponseEntity<Map<String, String>> invalidApple(AppleIdentityTokenVerifier.InvalidAppleTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_APPLE_TOKEN"));
    }

    @ExceptionHandler(com.kazka.auth.google.GoogleIdTokenVerifier.InvalidGoogleTokenException.class)
    public ResponseEntity<Map<String, String>> invalidGoogle(
            com.kazka.auth.google.GoogleIdTokenVerifier.InvalidGoogleTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_GOOGLE_TOKEN"));
    }

    @ExceptionHandler(MailDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleMailFailure(MailDeliveryException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "MAIL_SEND_FAILED"));
    }

    @ExceptionHandler(com.kazka.moderation.AccountSuspendedException.class)
    public ResponseEntity<Map<String, Object>> handleSuspended(com.kazka.moderation.AccountSuspendedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCOUNT_SUSPENDED"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WebExchangeBindException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fields.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION", "fields", fields));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_ARGUMENT"));
    }

    @ExceptionHandler(com.kazka.story.exception.PaywallRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePaywall(com.kazka.story.exception.PaywallRequiredException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "PAYWALL_REQUIRED");
        if (ex.getMessage() != null) body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }

    @ExceptionHandler(com.kazka.billing.AppleManagedSubscriptionException.class)
    public ResponseEntity<Map<String, Object>> handleAppleManaged(
            com.kazka.billing.AppleManagedSubscriptionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "APPLE_MANAGED"));
    }

    @ExceptionHandler(com.apple.itunes.storekit.verification.VerificationException.class)
    public ResponseEntity<Map<String, Object>> handleAppleVerification(
            com.apple.itunes.storekit.verification.VerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_SIGNATURE"));
    }

    private static String codeFor(ResponseStatusException ex) {
        int s = ex.getStatusCode().value();
        if (s == 404) return "NOT_FOUND";
        if (s == 401) return "UNAUTHENTICATED";
        if (s == 403) return "FORBIDDEN";
        return "ERROR";
    }
}
