package com.mazurek.eventOrganizer.auth.ratelimit;

import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientAddressResolver {
    private final AuthProperties authProperties;

    public String resolve(HttpServletRequest request) {
        if (authProperties.getRateLimit().isTrustForwardedHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
