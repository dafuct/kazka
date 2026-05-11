package com.kazka.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() { super("INVALID_REFRESH_TOKEN"); }
}
