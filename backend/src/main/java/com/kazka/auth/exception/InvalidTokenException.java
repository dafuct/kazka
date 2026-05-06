package com.kazka.auth.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token is missing, expired, or already used");
    }
}
