package com.mazurek.eventOrganizer.exception.auth;

public class EmailChangeAddressUnavailableException extends RuntimeException {
    public EmailChangeAddressUnavailableException() { super("This email address is unavailable."); }

    public EmailChangeAddressUnavailableException(Throwable cause) {
        super("This email address is unavailable.", cause);
    }
}
