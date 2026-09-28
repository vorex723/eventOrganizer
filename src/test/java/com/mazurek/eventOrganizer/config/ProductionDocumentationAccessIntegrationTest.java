package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.event.EventController;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@ActiveProfiles("production")
@Import({SecurityConfig.class, LocalDocumentationSecurityConfig.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class, ApiErrorResponseWriter.class})
class ProductionDocumentationAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void documentationIsDisabledAndDeniedInProductionProfile() throws Exception {
        assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isUnauthorized());
    }
}
