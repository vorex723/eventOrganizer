package com.mazurek.eventOrganizer.auth;

public enum EmailChangeResult {
    CHANGED("changed"), EXPIRED("expired"), INVALID_TOKEN("invalid_token"), EMAIL_UNAVAILABLE("email_unavailable");
    private final String redirectStatus;
    EmailChangeResult(String redirectStatus) { this.redirectStatus = redirectStatus; }
    public String getRedirectStatus() { return redirectStatus; }
}
