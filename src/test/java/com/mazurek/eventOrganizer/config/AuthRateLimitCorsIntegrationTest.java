package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.auth.AuthenticationController;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.ratelimit.AuthRateLimitStore;
import com.mazurek.eventOrganizer.auth.ratelimit.ClientAddressResolver;
import com.mazurek.eventOrganizer.auth.ratelimit.RateLimitDecision;
import com.mazurek.eventOrganizer.config.properties.ApiCorsProperties;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthenticationController.class, properties = {
        "app.auth.rate-limit.enabled=true",
        "app.api.cors.allowed-origins=https://app.example.com"
})
@Import({SecurityConfig.class, ApiCorsConfig.class, ClientAddressResolver.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class, ApiErrorResponseWriter.class})
@EnableConfigurationProperties({AuthProperties.class, ApiCorsProperties.class})
class AuthRateLimitCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;
    @MockitoBean
    private DeviceTypeResolver deviceTypeResolver;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private AuthRateLimitStore rateLimitStore;

    @Test
    void crossOriginRateLimitResponseExposesRetryAfter() throws Exception {
        when(rateLimitStore.tryConsume(anyString(), anyString(), anyInt(), any()))
                .thenReturn(new RateLimitDecision(false, 30));

        var response = mockMvc.perform(post("/api/v1/auth/register")
                        .header(HttpHeaders.ORIGIN, "https://app.example.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                .isNotNull()
                .satisfies(value -> assertThat(value.split(",\\s*"))
                        .contains("Location", "Retry-After"));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void disallowedOriginIsRejectedBeforeConsumingRateLimit() throws Exception {
        when(rateLimitStore.tryConsume(anyString(), anyString(), anyInt(), any()))
                .thenReturn(RateLimitDecision.permit());

        mockMvc.perform(post("/api/v1/auth/register")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        verifyNoInteractions(rateLimitStore, authenticationService);
    }

    @Test
    void corsPreflightDoesNotConsumeRateLimit() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header(HttpHeaders.ORIGIN, "https://app.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"));

        verifyNoInteractions(rateLimitStore, authenticationService);
    }
}
