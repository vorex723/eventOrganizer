package com.mazurek.eventOrganizer.exception.auth;

public class PasswordResetTokenNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Password reset token does not exist or has expired.";

    public PasswordResetTokenNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
