package com.mazurek.eventOrganizer.testSupport.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mazurek.eventOrganizer.testData.TestConstants.DatabaseConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TestDatabaseSafetyContext integration tests:")
class TestDatabaseSafetyContextIntegrationTest {
    @Test
    void whenSpringLoadsTestFactoriesShouldDiscoverGuardAutomatically() {
        assertThat(SpringFactoriesLoader.loadFactories(ContextCustomizerFactory.class, getClass().getClassLoader()))
                .anyMatch(factory -> factory instanceof TestDatabaseSafetyContextCustomizerFactory);
    }

    @Test
    void whenCustomizersAreCreatedForDifferentClassesShouldPreserveContextCacheEquality() {
        var factory = new TestDatabaseSafetyContextCustomizerFactory();
        assertThat(factory.createContextCustomizer(getClass(), List.of()))
                .isEqualTo(factory.createContextCustomizer(TestDatabaseSafetyUnitTest.class, List.of()));
    }

    @Test
    void whenContextHasNoDatabaseShouldNotRequireDatabaseConfiguration() {
        try (var context = new GenericApplicationContext()) {
            customize(context);
            context.registerBean(String.class, () -> "focused HTTP context");
            context.refresh();
            assertThat(context.getBean(String.class)).isEqualTo("focused HTTP context");
        }
    }

    @ParameterizedTest(name = "Rejected configuration: {0}")
    @ValueSource(strings = {
            "spring.datasource.url=jdbc:postgresql://foreign.invalid/event_organizer",
            "spring.datasource.username=postgres",
            "spring.datasource.hikari.jdbc-url=jdbc:postgresql://foreign.invalid/event_organizer",
            "spring.datasource.hikari.username=postgres",
            "spring.flyway.url=jdbc:postgresql://foreign.invalid/event_organizer",
            "spring.flyway.user=postgres",
            "spring.flyway.schemas=pg_catalog",
            "spring.datasource.hikari.schema=public,other",
            "spring.datasource.jndi-name=foreign-database",
            "spring.datasource.hikari.connection-init-sql=DELETE FROM users",
            "spring.datasource.hikari.connection-test-query=DELETE FROM users"
    })
    void whenEffectiveConfigurationIsUnsafeShouldFailBeforeCreatingSourceOrMutating(String override) {
        AtomicInteger sourceCreations = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();
        try (var context = new GenericApplicationContext()) {
            properties(context);
            customize(context);
            // Applied after customization, like a later dynamic test property source.
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, override);
            context.registerBean(DataSource.class, () -> {
                sourceCreations.incrementAndGet();
                return mock(DataSource.class);
            });
            context.registerBean(InitializingBean.class, () -> mutations::incrementAndGet);

            assertThatThrownBy(context::refresh).hasMessageStartingWith("Unsafe test database:");
            assertThat(sourceCreations).hasValue(0);
            assertThat(mutations).hasValue(0);
        }
    }

    @Test
    void whenActualSourceDiffersFromSafePropertiesShouldRejectWithoutConnecting() throws Exception {
        var source = spy(new DriverManagerDataSource(
                "jdbc:postgresql://foreign.invalid/event_organizer", TEST_DATABASE_USERNAME, "unused"));
        try (var context = new GenericApplicationContext()) {
            properties(context);
            customize(context);
            context.registerBean(DataSource.class, () -> source);

            assertThatThrownBy(context::refresh).hasRootCauseMessage(
                    "Unsafe test database: only a single explicit host and event_organizer_test database are allowed");
            verify(source, never()).getConnection();
        }
    }

    @Test
    void whenFlywayUsesSeparateUnsafeSourceShouldRejectBeforeMigrationInitializer() throws Exception {
        var source = spy(new DriverManagerDataSource(
                "jdbc:postgresql://foreign.invalid/event_organizer", TEST_DATABASE_USERNAME, "unused"));
        Flyway flyway = mock(Flyway.class);
        Configuration configuration = mock(Configuration.class);
        when(flyway.getConfiguration()).thenReturn(configuration);
        when(configuration.getDataSource()).thenReturn(source);
        AtomicInteger migrations = new AtomicInteger();
        try (var context = new GenericApplicationContext()) {
            properties(context);
            customize(context);
            context.registerBean(Flyway.class, () -> flyway);
            context.registerBean(FlywayMigrationInitializer.class,
                    () -> new FlywayMigrationInitializer(context.getBean(Flyway.class), ignored -> migrations.incrementAndGet()));

            assertThatThrownBy(context::refresh).hasRootCauseMessage(
                    "Unsafe test database: only a single explicit host and event_organizer_test database are allowed");
            assertThat(migrations).hasValue(0);
            verify(source, never()).getConnection();
            verify(flyway, never()).migrate();
        }
    }

    private void properties(GenericApplicationContext context) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "spring.datasource.url=" + TEST_DATABASE_URL,
                "spring.datasource.username=" + TEST_DATABASE_USERNAME);
    }

    private void customize(GenericApplicationContext context) {
        new TestDatabaseSafetyContextCustomizerFactory().createContextCustomizer(getClass(), List.of())
                .customizeContext(context, mock(MergedContextConfiguration.class));
    }
}
