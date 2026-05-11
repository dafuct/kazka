package com.kazka.auth.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("INVALID_CREDENTIALS"); }
}
