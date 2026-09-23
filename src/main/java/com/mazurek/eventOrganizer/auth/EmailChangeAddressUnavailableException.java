package com.mazurek.eventOrganizer.auth;

public class EmailChangeAddressUnavailableException extends RuntimeException {
    public EmailChangeAddressUnavailableException() { super("This email address is unavailable."); }
}
