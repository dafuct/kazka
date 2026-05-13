package com.kazka.story.exception;

public class PaywallRequiredException extends RuntimeException {
    public PaywallRequiredException(String message) {
        super(message);
    }
}
