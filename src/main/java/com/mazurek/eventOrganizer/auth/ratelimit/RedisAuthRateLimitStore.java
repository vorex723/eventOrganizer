package com.mazurek.eventOrganizer.auth.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Profile("production")
public class RedisAuthRateLimitStore implements AuthRateLimitStore {
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current and tonumber(current) >= tonumber(ARGV[1]) then return 0 end
            local value = redis.call('INCR', KEYS[1])
            if value == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisAuthRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitDecision tryConsume(String bucket, String subjectHash, int limit, Duration window) {
        String key = "event-organizer:auth-rate-limit:" + bucket + ':' + subjectHash;
        long seconds = Math.max(1, window.toSeconds());
        Long consumed = redisTemplate.execute(CONSUME_SCRIPT, List.of(key), Integer.toString(limit), Long.toString(seconds));
        if (Long.valueOf(1).equals(consumed)) return RateLimitDecision.permit();
        Long remaining = redisTemplate.getExpire(key);
        return new RateLimitDecision(false, remaining == null || remaining < 1 ? 1 : remaining);
    }
}
