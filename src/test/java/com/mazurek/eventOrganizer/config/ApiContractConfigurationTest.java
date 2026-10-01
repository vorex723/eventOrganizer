package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.config.properties.ApiCorsProperties;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticationEntryPointWritesStableJsonEnvelope() throws Exception {
        ApiErrorResponseWriter writer = new ApiErrorResponseWriter(objectMapper);
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(writer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        var body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("status").asInt()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(body.path("code").asString()).isEqualTo(ApiErrorCode.INVALID_ACCESS_TOKEN);
        assertThat(body.path("message").asString()).isEqualTo("The access token is invalid or expired.");
        assertThat(body.path("errors").isNull()).isTrue();
    }

    @Test
    void accessDeniedHandlerWritesStableJsonEnvelope() throws Exception {
        ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler(new ApiErrorResponseWriter(objectMapper));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, null);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        var body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("status").asInt()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(body.path("code").asString()).isEqualTo(ApiErrorCode.ACCESS_DENIED);
        assertThat(body.path("message").asString()).isEqualTo("You do not have permission to access this resource.");
        assertThat(body.path("errors").isNull()).isTrue();
    }

    @Test
    void corsUsesOnlyExplicitOrigins() {
        ApiCorsProperties properties = new ApiCorsProperties();
        properties.setAllowedOrigins(List.of("https://app.example.com"));
        ApiCorsConfig config = new ApiCorsConfig(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/events");
        request.addHeader("Origin", "https://app.example.com");

        var cors = config.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(cors.getAllowedOrigins()).containsExactly("https://app.example.com");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "X-Device-Type");
        assertThat(cors.getAllowCredentials()).isFalse();
    }

    @Test
    void openApiMarksPublicReadsPublicAndMutationsBearerProtected() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openApi = config.openAPI();
        Operation auth = new Operation();
        Operation localDevelopment = new Operation();
        Operation publicEventRead = new Operation();
        Operation protectedOperation = new Operation();
        openApi.setPaths(new Paths()
                .addPathItem("/api/v1/auth/login", new PathItem().post(auth))
                .addPathItem("/api/v1/dev/auth-emails", new PathItem().get(localDevelopment))
                .addPathItem("/api/v1/events", new PathItem().get(publicEventRead).post(protectedOperation)));

        config.apiContractCustomizer(new MockEnvironment()).customise(openApi);

        assertThat(openApi.getSecurity()).singleElement().extracting(requirement -> requirement.get("bearerAuth"))
                .isEqualTo(List.of());
        assertThat(auth.getSecurity()).isEmpty();
        assertThat(auth.getResponses()).containsKeys("400", "500").doesNotContainKeys("401", "403");
        assertThat(localDevelopment.getSecurity()).isNull();
        assertThat(localDevelopment.getResponses()).containsKeys("401", "403");
        assertThat(publicEventRead.getSecurity()).isEmpty();
        assertThat(publicEventRead.getResponses()).containsKeys("400", "500").doesNotContainKeys("401", "403");
        assertThat(protectedOperation.getSecurity()).isNull();
        assertThat(protectedOperation.getResponses()).containsKeys("400", "401", "403", "500");
        assertThat(openApi.getComponents().getSchemas()).containsKey("ApiError");
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "production", "test", "default", "local,production"})
    void openApiMakesOnlyTheLocalInboxReadPublic(String profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles.split(","));
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openApi = config.openAPI();
        Operation inboxRead = new Operation();
        Operation inboxWrite = new Operation();
        Operation unexpectedDevelopmentRead = new Operation();
        openApi.setPaths(new Paths()
                .addPathItem("/api/v1/dev/auth-emails", new PathItem().get(inboxRead).post(inboxWrite))
                .addPathItem("/api/v1/dev/unexpected", new PathItem().get(unexpectedDevelopmentRead)));

        config.apiContractCustomizer(environment).customise(openApi);

        if (profiles.equals("local")) {
            assertThat(inboxRead.getSecurity()).isEmpty();
            assertThat(inboxRead.getResponses()).doesNotContainKeys("401", "403");
        } else {
            assertThat(inboxRead.getSecurity()).isNull();
            assertThat(inboxRead.getResponses()).containsKeys("401", "403");
        }
        assertThat(inboxWrite.getSecurity()).isNull();
        assertThat(inboxWrite.getResponses()).containsKeys("401", "403");
        assertThat(unexpectedDevelopmentRead.getSecurity()).isNull();
        assertThat(unexpectedDevelopmentRead.getResponses()).containsKeys("401", "403");
    }
}
