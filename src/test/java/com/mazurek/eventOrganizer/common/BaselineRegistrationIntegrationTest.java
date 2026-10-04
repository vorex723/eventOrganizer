package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Uses a separate schema and deliberately never invokes AuthHelper or DeletionService. */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BaselineRegistrationIntegrationTest {
    private static final String SCHEMA = "baseline_registration_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.schema", () -> SCHEMA);
    }

    @Autowired private AuthenticationService authenticationService;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private ActivationTokenRepository activationTokens;
    @Autowired private DataSource dataSource;

    @Test
    void firstRegistrationUsesOnlyRolesProvisionedByBaseline() {
        assertThat(users.count()).isZero();
        assertThat(roles.findAll()).extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");

        var request = RegisterRequestTestBuilder.firstUserRegisterRequest().build();
        authenticationService.register(request);

        var saved = users.findByEmail(request.getEmail()).orElseThrow();
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");
        assertThat(saved.isActivated()).isFalse();
        assertThat(saved.getHomeCity()).isNotNull();
        assertThat(activationTokens.count()).isEqualTo(1);
    }

    @AfterAll
    void removeOwnedBootstrapSchema() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            connection.setSchema("public");
            statement.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
        }
    }
}
