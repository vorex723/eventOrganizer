package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Isolate the security configuration to verify its guard independently of startup profile validation.
@ActiveProfiles({"local", "production"})
@DisplayName("Mixed profile development security integration tests:")
class MixedProfileDevelopmentSecurityIntegrationTest extends DevelopmentEndpointSecurityTestSupport {

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
    void whenProductionAndLocalAreMixedShouldDisablePublicInboxException() throws Exception {
        assertThat(context.containsBean("localDevelopmentSecurityFilterChain")).isFalse();
        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.ACCESS_DENIED));
        verifyNoInteractions(sink);
    }
}
