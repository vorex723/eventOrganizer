package com.mazurek.eventOrganizer.auth.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAuthRateLimitStoreUnitTest {
    @Test
    void limitsOneBucketWithoutAffectingAnotherBucket() {
        InMemoryAuthRateLimitStore store = new InMemoryAuthRateLimitStore();

        assertThat(store.tryConsume("login", "first", 1, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(store.tryConsume("login", "first", 1, Duration.ofMinutes(1)).allowed()).isFalse();
        assertThat(store.tryConsume("register", "first", 1, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(store.tryConsume("login", "second", 1, Duration.ofMinutes(1)).allowed()).isTrue();
    }
}
