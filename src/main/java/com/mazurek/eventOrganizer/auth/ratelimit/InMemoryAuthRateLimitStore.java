package com.mazurek.eventOrganizer.auth.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Profile("!production")
public class InMemoryAuthRateLimitStore implements AuthRateLimitStore {
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision tryConsume(String bucket, String subjectHash, int limit, Duration window) {
        Instant now = Instant.now();
        WindowCounter counter = counters.compute(bucket + ':' + subjectHash, (key, current) -> {
            if (current == null || !now.isBefore(current.expiresAt)) return new WindowCounter(1, now.plus(window));
            return new WindowCounter(current.count + 1, current.expiresAt);
        });
        if (counter.count <= limit) return RateLimitDecision.permit();
        return new RateLimitDecision(false, Math.max(1, Duration.between(now, counter.expiresAt).toSeconds()));
    }

    private record WindowCounter(int count, Instant expiresAt) { }
}
