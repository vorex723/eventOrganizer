package com.mazurek.eventOrganizer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DisplayName("Test profile development security integration tests:")
class TestProfileDevelopmentSecurityIntegrationTest extends DevelopmentEndpointSecurityTestSupport {

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
    void whenTestProfileRunsShouldExcludeLocalInboxAndSecurityChain() throws Exception {
        assertInboxDeniedOutsideLocalProfile();
    }
}
