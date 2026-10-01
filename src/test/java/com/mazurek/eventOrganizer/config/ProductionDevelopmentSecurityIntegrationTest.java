package com.mazurek.eventOrganizer.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("production")
class ProductionDevelopmentSecurityIntegrationTest extends DevelopmentEndpointSecurityTestSupport {

    @Test
    void localInboxAndItsPublicSecurityChainAreAbsentInProduction() throws Exception {
        assertInboxDeniedOutsideLocalProfile();
    }
}
