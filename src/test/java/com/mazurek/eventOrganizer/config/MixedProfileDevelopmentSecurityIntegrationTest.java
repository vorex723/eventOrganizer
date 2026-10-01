package com.mazurek.eventOrganizer.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Isolate the security configuration to verify its guard independently of startup profile validation.
@ActiveProfiles({"local", "production"})
class MixedProfileDevelopmentSecurityIntegrationTest extends DevelopmentEndpointSecurityTestSupport {

    @Test
    void productionDisablesTheLocalExceptionEvenWhenLocalIsAlsoActive() throws Exception {
        assertThat(context.containsBean("localDevelopmentSecurityFilterChain")).isFalse();
        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sink);
    }
}
