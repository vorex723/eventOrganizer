package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.auth.email.LocalAuthEmailController;
import com.mazurek.eventOrganizer.auth.email.LocalAuthEmailSink;
import com.mazurek.eventOrganizer.event.EventController;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = EventController.class)
@ActiveProfiles("local")
@Import({SecurityConfig.class, LocalDocumentationSecurityConfig.class, LocalDevelopmentSecurityConfig.class,
        LocalAuthEmailController.class, OpenApiConfig.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class, ApiErrorResponseWriter.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class, SwaggerConfig.class})
@EnableConfigurationProperties({SpringDocConfigProperties.class, SwaggerUiConfigProperties.class,
        SwaggerUiOAuthProperties.class})
@DisplayName("LocalDocumentationAccessIntegrationTest contracts:")
class LocalDocumentationAccessIntegrationTest {

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
    @MockitoBean
    private LocalAuthEmailSink localAuthEmailSink;

    @Test
    void whenLocalDocumentationIsRequestedAnonymouslyShouldAllowRead() throws Exception {
        assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isTrue();

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/dev/auth-emails'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/dev/auth-emails'].get.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/dev/auth-emails'].get.responses['403']").doesNotExist());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
