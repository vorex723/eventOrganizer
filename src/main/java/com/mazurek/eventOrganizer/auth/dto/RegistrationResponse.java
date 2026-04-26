package com.mazurek.eventOrganizer.auth.dto;

public record RegistrationResponse(String message) {

    public static final String VERIFICATION_REQUIRED_MESSAGE = "Verify your email to get access.";

    public static RegistrationResponse verificationRequired() {
        return new RegistrationResponse(VERIFICATION_REQUIRED_MESSAGE);
    }
}
