package com.mazurek.eventOrganizer.auth.ratelimit;

import java.time.Duration;

public interface AuthRateLimitStore {
    RateLimitDecision tryConsume(String bucket, String subjectHash, int limit, Duration window);
}
