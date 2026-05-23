package com.kazka.billing;

public class AppleManagedSubscriptionException extends RuntimeException {
    public AppleManagedSubscriptionException() {
        super("Apple-managed subscription must be cancelled via App Store");
    }
}
