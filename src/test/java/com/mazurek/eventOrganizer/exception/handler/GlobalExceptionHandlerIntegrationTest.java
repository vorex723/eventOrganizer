package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.auth.AuthenticationController;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.exception.user.UserRoleNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtRequestFilter;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, UserExceptionHandler.class})
@DisplayName("Global exception handler integration tests:")
class GlobalExceptionHandlerIntegrationTest {

    private static final String SENSITIVE_RUNTIME_MESSAGE = "sensitive internal runtime details";
    private static final String SENSITIVE_ROLE_MESSAGE = "role lookup failed in private bootstrap path";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private AuthProperties authProperties;

    @MockitoBean
    private DeviceTypeResolver deviceTypeResolver;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    private String registerRequestJson() throws Exception {
        return objectMapper.writeValueAsString(RegisterRequestTestBuilder.thirdUserRegisterRequest().build());
    }

    @Test
    @DisplayName("When unexpected runtime exception occurs should return generic HTTP 500 message")
    void whenUnexpectedRuntimeExceptionOccursShouldReturnGenericHttp500Message() throws Exception {
        doThrow(new RuntimeException(SENSITIVE_RUNTIME_MESSAGE))
                .when(authenticationService)
                .register(any());

        mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(BaseDomainExceptionHandler.GENERIC_INTERNAL_ERROR_MESSAGE))
                .andExpect(jsonPath("$.message", not(containsString(SENSITIVE_RUNTIME_MESSAGE))));
    }

    @Test
    @DisplayName("When user role resolution fails should return generic HTTP 500 message")
    void whenUserRoleResolutionFailsShouldReturnGenericHttp500Message() throws Exception {
        doThrow(new UserRoleNotFoundException(SENSITIVE_ROLE_MESSAGE))
                .when(authenticationService)
                .register(any());

        mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(BaseDomainExceptionHandler.GENERIC_INTERNAL_ERROR_MESSAGE))
                .andExpect(jsonPath("$.message", not(containsString(SENSITIVE_ROLE_MESSAGE))));
    }
}
