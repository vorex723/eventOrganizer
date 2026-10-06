package com.mazurek.eventOrganizer.auth.ratelimit;

import com.mazurek.eventOrganizer.testData.builders.AuthPropertiesTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.AuthRateLimitPropertiesTestBuilder;
import com.mazurek.eventOrganizer.config.ApiErrorResponseWriter;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.AUTH_REGISTER_URL;

@DisplayName("AuthRateLimitFilter unit tests:")
class AuthRateLimitFilterUnitTest {
    @Test
    void whenRegistrationSourceLimitIsReachedShouldRejectFurtherRequest() throws Exception {
        AuthProperties properties = new AuthPropertiesTestBuilder()
                .rateLimit(new AuthRateLimitPropertiesTestBuilder()
                        .keySecret("test-key")
                        .registrationMaxRequests(1)
                        .registrationWindow(Duration.ofMinutes(1))
                        .build())
                .build();
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
        MockHttpServletRequest request = new MockHttpServletRequest("POST", AUTH_REGISTER_URL);
        request.setRemoteAddr("192.0.2.15");
        return request;
    }
}
