package com.mazurek.eventOrganizer.auth;

public enum ActivationResult {
    ACTIVATED("activated"),
    TOKEN_EXPIRED_NEW_SENT("expired_resent"),
    INVALID_TOKEN("invalid_token");

    private final String redirectStatus;

    ActivationResult(String redirectStatus) {
        this.redirectStatus = redirectStatus;
    }

    public String getRedirectStatus() {
        return redirectStatus;
    }
}
