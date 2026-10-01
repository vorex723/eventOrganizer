package com.mazurek.eventOrganizer.exception.auth;

public class EmailChangeTokenInvalidException extends RuntimeException {
    public EmailChangeTokenInvalidException() {
        super("Email-change token is invalid or has already been used.");
    }
}
