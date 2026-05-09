package com.kazka.moderation;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException() { super("ACCOUNT_SUSPENDED"); }
}
