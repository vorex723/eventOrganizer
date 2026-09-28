package com.mazurek.eventOrganizer.auth.ratelimit;

import com.mazurek.eventOrganizer.config.ApiErrorResponseWriter;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterUnitTest {
    @Test
    void rejectsARegistrationAfterItsPerSourceLimitIsReached() throws Exception {
        AuthProperties properties = new AuthProperties();
        AuthProperties.RateLimit settings = properties.getRateLimit();
        settings.setKeySecret("test-key");
        settings.setRegistrationMaxRequests(1);
        settings.setRegistrationWindow(Duration.ofMinutes(1));
        AuthRateLimitFilter filter = new AuthRateLimitFilter(
                properties,
                new InMemoryAuthRateLimitStore(),
                new ClientAddressResolver(properties),
                new ApiErrorResponseWriter(new ObjectMapper())
        );
        AtomicInteger chainCalls = new AtomicInteger();

        MockHttpServletRequest firstRequest = registrationRequest();
        filter.doFilter(firstRequest, new MockHttpServletResponse(), (request, response) -> chainCalls.incrementAndGet());

        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        filter.doFilter(registrationRequest(), limitedResponse, (request, response) -> chainCalls.incrementAndGet());

        assertThat(chainCalls).hasValue(1);
        assertThat(limitedResponse.getStatus()).isEqualTo(429);
        assertThat(limitedResponse.getHeader("Retry-After")).isNotBlank();
        var body = new ObjectMapper().readTree(limitedResponse.getContentAsByteArray());
        assertThat(body.path("status").asInt()).isEqualTo(429);
        assertThat(body.path("code").asString()).isEqualTo(ApiErrorCode.RATE_LIMITED);
        assertThat(body.path("message").asString()).isEqualTo("Too many authentication requests. Please try again later.");
        assertThat(body.path("errors").isNull()).isTrue();
    }

    private MockHttpServletRequest registrationRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        request.setRemoteAddr("192.0.2.15");
        return request;
    }
}
