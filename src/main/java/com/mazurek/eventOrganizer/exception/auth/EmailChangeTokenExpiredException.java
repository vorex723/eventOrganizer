package com.mazurek.eventOrganizer.exception.auth;

public class EmailChangeTokenExpiredException extends RuntimeException {
    public EmailChangeTokenExpiredException() {
        super("Email-change token has expired. Request a new confirmation email.");
    }
}
