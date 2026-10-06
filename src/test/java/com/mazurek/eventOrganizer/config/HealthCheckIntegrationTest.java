package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.auth.email.worker-enabled=false", "server.address=127.0.0.1"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HealthCheckIntegrationTest contracts:")
class HealthCheckIntegrationTest {

    @DynamicPropertySource
    static void loadActualManagementConfiguration(DynamicPropertyRegistry registry) throws IOException {
        // Test resources shadow the main application.properties; load only health configuration,
        // leaving test database credentials, encryption keys and worker settings intact.
        var properties = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource("src/main/resources/application.properties"));
        properties.stringPropertyNames().stream().filter(key -> key.startsWith("management."))
                .forEach(key -> registry.add(key, () -> properties.getProperty(key)));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private HealthContributorRegistry healthContributors;
    @Autowired private WebEndpointsSupplier webEndpoints;
    @LocalServerPort private int port;
    @MockitoSpyBean(name = "dbHealthContributor") private HealthContributor databaseHealth;
    @MockitoSpyBean private JwtUtils jwtUtils;
    @MockitoSpyBean private UserRepository userRepository;

    @Test
    void whenReadingHealthAnonymouslyShouldReportDatabaseWithoutDetails() throws Exception {
        assertThat(healthContributors.stream().map(HealthContributors.Entry::name)).containsExactly("db");
        assertThat(webEndpoints.getEndpoints()).extracting(endpoint -> endpoint.getEndpointId().toString())
                .containsExactly("health");

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
    }

    @Test
    void whenDatabaseIsDownShouldReturnUnavailableWithoutDetails() throws Exception {
        doReturn(Health.down()
                .withException(new IllegalStateException("private database connection details"))
                .withDetail("database", "PostgreSQL")
                .build()).when((HealthIndicator) databaseHealth).health();

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"DOWN\"}", JsonCompareMode.STRICT));
        assertHeadResponse(503);
    }

    @Test
    void whenHealthHasValidBearerShouldNotRevealDetails() throws Exception {
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, adminBearerToken()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
    }

    @Test
    void whenHealthHasBearerHeadersShouldSkipJwtAndUserLookup() throws Exception {
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
        assertHeadResponse(200);

        verifyNoInteractions(jwtUtils, userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/metrics", "/actuator/info",
            "/actuator/health/db", "/actuator/health/liveness", "/actuator/health/readiness", "/actuator/health/"})
    void whenOtherActuatorPathsAreRequestedAnonymouslyShouldDenyAccess(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/metrics", "/actuator/info",
            "/actuator/health/db", "/actuator/health/liveness", "/actuator/health/readiness", "/actuator/health/"})
    void whenOtherActuatorPathsAreRequestedByAdminShouldDenyAccess(String path) throws Exception {
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, adminBearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.ACCESS_DENIED));
    }

    @Test
    void whenHealthUsesPostOrOptionsShouldDenyAccess() throws Exception {
        String bearerToken = adminBearerToken();
        mockMvc.perform(post("/actuator/health").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.ACCESS_DENIED));
        mockMvc.perform(options("/actuator/health").header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.ACCESS_DENIED));
    }

    @Test
    void whenDomainEndpointIsRequestedAnonymouslyShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
    }

    private String adminBearerToken() {
        User admin = new UserTestBuilder()
                .firstName(null)
                .lastName(null)
                .password(null)
                .homeCity(null)
                .timeZone(null)
                .createdAt(null)
                .lastCredentialsChangeTime(null)
                .id(UUID.randomUUID())
                .email("health-admin@example.com")
                .activated(true)
                .roles(Set.of(new RoleTestBuilder()
                        .id(null)
                        .name("ROLE_ADMIN")
                        .build()))
                .build();
        doReturn(Optional.of(admin)).when(userRepository).findById(admin.getId());
        return "Bearer " + jwtUtils.generateAccessToken(admin);
    }

    private void assertHeadResponse(int expectedStatus) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .timeout(Duration.ofSeconds(10))
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(expectedStatus);
            assertThat(response.body()).isEmpty();
        }
    }
}
