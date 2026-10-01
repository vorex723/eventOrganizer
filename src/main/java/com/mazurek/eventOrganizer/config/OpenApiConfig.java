package com.mazurek.eventOrganizer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {
    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/api/v1/events",
            "/api/v1/events/{eventId}",
            "/api/v1/cities/{cityName}",
            "/api/v1/cities/{cityName}/events",
            "/api/v1/tags/{tagName}",
            "/api/v1/tags/{tagName}/events"
    );

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Organizer API")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSchemas("ApiError", new ObjectSchema()
                                .addProperty("status", new IntegerSchema().example(400))
                                .addProperty("code", new StringSchema().example("VALIDATION_FAILED"))
                                .addProperty("message", new StringSchema().example("Request validation failed"))
                                .addProperty("errors", new ObjectSchema()
                                        .description("Optional map of field names to validation messages."))))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer apiContractCustomizer(Environment environment) {
        boolean localDevelopment = environment.matchesProfiles("local & !production");
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
                if (path.startsWith("/api/v1/auth/")
                        || (localDevelopment && method == PathItem.HttpMethod.GET
                            && path.equals(SecurityConfig.LOCAL_AUTH_EMAILS_PATH))
                        || (method == PathItem.HttpMethod.GET && PUBLIC_GET_PATHS.contains(path))) {
                    operation.setSecurity(List.of());
                    addPublicErrorResponses(operation);
                    return;
                }
                addProtectedErrorResponses(operation);
            }));
        };
    }

    private void addPublicErrorResponses(Operation operation) {
        ApiResponses responses = getOrCreateResponses(operation);
        addErrorResponse(responses, "400", "Invalid request");
        addErrorResponse(responses, "500", "Unexpected server error");
    }

    private void addProtectedErrorResponses(Operation operation) {
        ApiResponses responses = getOrCreateResponses(operation);
        addErrorResponse(responses, "400", "Invalid request");
        addErrorResponse(responses, "401", "Authentication required or access token invalid");
        addErrorResponse(responses, "403", "Access denied");
        addErrorResponse(responses, "500", "Unexpected server error");
    }

    private ApiResponses getOrCreateResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        return responses;
    }

    private void addErrorResponse(ApiResponses responses, String status, String description) {
        responses.putIfAbsent(status, new ApiResponse()
                .description(description)
                .content(new io.swagger.v3.oas.models.media.Content().addMediaType(
                        "application/json",
                        new io.swagger.v3.oas.models.media.MediaType().schema(new ObjectSchema().$ref("#/components/schemas/ApiError"))
                )));
    }
}
