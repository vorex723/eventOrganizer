package com.mazurek.eventOrganizer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;

@Configuration
public class OpenApiConfig {

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
                                .addProperty("errors", new MapSchema().additionalProperties(new StringSchema()))))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer apiContractCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> {
                if (path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/dev/")) {
                    operation.setSecurity(List.of());
                }
                addSharedErrorResponses(operation);
            }));
        };
    }

    private void addSharedErrorResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        addErrorResponse(responses, "400", "Invalid request");
        addErrorResponse(responses, "401", "Authentication required or access token invalid");
        addErrorResponse(responses, "403", "Access denied");
        addErrorResponse(responses, "500", "Unexpected server error");
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
