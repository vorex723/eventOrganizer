package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.testData.builders.LocalAuthEmailTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("local")
@DisplayName("Local development security integration tests:")
class LocalDevelopmentSecurityIntegrationTest extends DevelopmentEndpointSecurityTestSupport {

    @Nested
    @DisplayName("Development access tests:")
    class DevelopmentAccessTests {

        @ParameterizedTest(name = "[{index}] anonymous path={0}")
        @ValueSource(strings = {"/api/v1/dev", "/api/v1/dev/unexpected",
                "/api/v1/dev/auth-emails/nested", "/api/v1/dev/auth-emails/"})
        void whenOtherDevelopmentPathRequestedAnonymouslyShouldDenyAccess(String path) throws Exception {
            assertOtherDevelopmentPathDeniedAnonymously(path);
        }

        @ParameterizedTest(name = "[{index}] authenticated path={0}")
        @ValueSource(strings = {"/api/v1/dev", "/api/v1/dev/unexpected",
                "/api/v1/dev/auth-emails/nested", "/api/v1/dev/auth-emails/"})
        void whenOtherDevelopmentPathRequestedWithValidBearerTokenShouldDenyAccess(String path) throws Exception {
            assertOtherDevelopmentPathDeniedWithValidBearerToken(path);
        }

        @ParameterizedTest(name = "[{index}] HTTP method={0}")
        @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"})
        void whenInboxRequestedWithMethodOtherThanGetShouldDenyAccess(String method) throws Exception {
            assertInboxMethodOtherThanGetDenied(method);
        }
    }

    @Test
    void whenLocalInboxIsRequestedAnonymouslyShouldAllowRead() throws Exception {
        assertThat(context.containsBean("localDevelopmentSecurityFilterChain")).isTrue();
        when(sink.recent()).thenReturn(List.of(new LocalAuthEmailTestBuilder()
                .type(AuthEmailType.ACCOUNT_ACTIVATION)
                .recipientEmail("developer@example.com")
                .link("https://localhost/activate?token=test-token")
                .sentAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build()));

        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientEmail").value("developer@example.com"))
                .andExpect(jsonPath("$[0].link").value("https://localhost/activate?token=test-token"));
        verify(sink).recent();
    }

    @Test
    void whenLocalInboxUsesCorsShouldAllowReadsWithoutSession() throws Exception {
        mockMvc.perform(options(SecurityConfig.LOCAL_AUTH_EMAILS_PATH)
                        .header(HttpHeaders.ORIGIN, "https://app.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"));
        verifyNoInteractions(sink);

        var result = mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH)
                        .header(HttpHeaders.ORIGIN, "https://app.example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void whenLocalInboxOriginIsUntrustedShouldRejectRequest() throws Exception {
        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH)
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        verifyNoInteractions(sink);
    }
}
