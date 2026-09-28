package com.mazurek.eventOrganizer.auth.ratelimit;

public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    public static RateLimitDecision permit() { return new RateLimitDecision(true, 0); }
}
