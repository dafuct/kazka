package com.kazka.auth.exception;

public class MailDeliveryException extends RuntimeException {
    public MailDeliveryException(Throwable cause) {
        super("Failed to send email", cause);
    }
}
