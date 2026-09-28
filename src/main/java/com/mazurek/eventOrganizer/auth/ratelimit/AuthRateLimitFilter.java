package com.mazurek.eventOrganizer.auth.ratelimit;

import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final AuthProperties authProperties;
    private final AuthRateLimitStore authRateLimitStore;
    private final ClientAddressResolver clientAddressResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!authProperties.getRateLimit().isEnabled() || !HttpMethod.POST.matches(request.getMethod())) return true;
        String path = request.getRequestURI();
        return !"/api/v1/auth/register".equals(path) && !"/api/v1/auth/login".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AuthProperties.RateLimit settings = authProperties.getRateLimit();
        boolean registration = "/api/v1/auth/register".equals(request.getRequestURI());
        RateLimitDecision decision = authRateLimitStore.tryConsume(
                registration ? "register" : "login",
                hash(clientAddressResolver.resolve(request)),
                registration ? settings.getRegistrationMaxRequests() : settings.getLoginMaxRequests(),
                registration ? settings.getRegistrationWindow() : settings.getLoginWindow()
        );
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":429,\"message\":\"Too many authentication requests. Please try again later.\"}");
    }

    private String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(authProperties.getRateLimit().getKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 must be available.", exception);
        }
    }
}
