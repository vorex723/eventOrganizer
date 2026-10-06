package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.ratelimit.RateLimitDecision;

import static com.mazurek.eventOrganizer.testData.TestConstants.RateLimitFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class RateLimitDecisionTestBuilder {
    private boolean allowed = RateLimitFixtureConstants.ALLOWED;
    private long retryAfterSeconds = RateLimitFixtureConstants.RETRY_AFTER_SECONDS;

    public RateLimitDecisionTestBuilder allowed(boolean allowed) {
        this.allowed = allowed;
        return this;
    }

    public RateLimitDecisionTestBuilder retryAfterSeconds(long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
        return this;
    }


    public RateLimitDecision build() {
        return new RateLimitDecision(
                allowed,
                retryAfterSeconds
        );
    }
}
