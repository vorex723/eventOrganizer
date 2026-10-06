package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.auth.email.LocalAuthEmailController;
import com.mazurek.eventOrganizer.auth.email.LocalAuthEmailSink;
import com.mazurek.eventOrganizer.config.properties.ApiCorsProperties;
import com.mazurek.eventOrganizer.jwt.JwtRequestFilter;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DevelopmentEndpointSecurityTestSupport.DevelopmentProbeController.class,
        properties = "app.api.cors.allowed-origins=https://app.example.com")
@Import({SecurityConfig.class, LocalDevelopmentSecurityConfig.class, ApiCorsConfig.class,
        LocalAuthEmailController.class, ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class, ApiErrorResponseWriter.class,
        DevelopmentEndpointSecurityTestSupport.DevelopmentProbeController.class,
        DevelopmentEndpointSecurityTestSupport.FilterConfiguration.class})
@EnableConfigurationProperties(ApiCorsProperties.class)
abstract class DevelopmentEndpointSecurityTestSupport {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ApplicationContext context;
    @MockitoBean protected JwtUtils jwtUtils;
    @MockitoBean protected UserRepository userRepository;
    @MockitoBean protected LocalAuthEmailSink sink;

    protected void assertOtherDevelopmentPathDeniedAnonymously(String path) throws Exception {
        assertThat(context.getBeansOfType(DevelopmentProbeController.class)).hasSize(1);
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        verifyNoInteractions(sink);
    }

    protected void assertOtherDevelopmentPathDeniedWithValidBearerToken(String path) throws Exception {
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(sink);
    }

    protected void assertInboxMethodOtherThanGetDenied(String method) throws Exception {
        ResultActions anonymous = mockMvc.perform(request(HttpMethod.valueOf(method), SecurityConfig.LOCAL_AUTH_EMAILS_PATH))
                .andExpect(status().isUnauthorized());
        ResultActions authenticated = mockMvc.perform(request(HttpMethod.valueOf(method), SecurityConfig.LOCAL_AUTH_EMAILS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden());
        // Preserve body-less HEAD and the separately governed OPTIONS contract.
        if (!method.equals("HEAD") && !method.equals("OPTIONS")) {
            anonymous.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
            authenticated.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
        verifyNoInteractions(sink);
    }

    protected void assertInboxDeniedOutsideLocalProfile() throws Exception {
        assertThat(context.containsBean("localDevelopmentSecurityFilterChain")).isFalse();
        assertThat(context.getBeansOfType(LocalAuthEmailController.class)).isEmpty();
        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get(SecurityConfig.LOCAL_AUTH_EMAILS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(sink);
    }

    protected String bearerToken() {
        var user = UserTestBuilder.firstUser().build();
        when(jwtUtils.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtils.extractUsername("valid-token")).thenReturn(user.getEmail());
        when(jwtUtils.extractUserId("valid-token")).thenReturn(user.getId());
        when(jwtUtils.extractSecurityVersion("valid-token")).thenReturn(user.getSecurityVersion());
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(jwtUtils).extractAuthorities("valid-token");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        return "Bearer valid-token";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FilterConfiguration {
        @Bean
        FilterRegistrationBean<JwtRequestFilter> jwtFilterRegistration(JwtRequestFilter filter) {
            // MockMvc must run the JWT filter inside the selected security chain only.
            FilterRegistrationBean<JwtRequestFilter> registration = new FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }
    }

    @TestComponent
    @RestController
    static class DevelopmentProbeController {
        @RequestMapping({"/api/v1/dev", "/api/v1/dev/unexpected", "/api/v1/dev/auth-emails/nested"})
        String developmentProbe() {
            return "This development controller must never be accessible.";
        }
    }
}
