package com.mazurek.eventOrganizer.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class TestProfileDevelopmentSecurityIntegrationTest extends DevelopmentEndpointSecurityTestSupport {

    @Test
    void localInboxAndItsPublicSecurityChainAreAbsentInTestProfile() throws Exception {
        assertInboxDeniedOutsideLocalProfile();
    }
}
