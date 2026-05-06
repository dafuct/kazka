package com.kazka.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Email is not verified");
    }
}
