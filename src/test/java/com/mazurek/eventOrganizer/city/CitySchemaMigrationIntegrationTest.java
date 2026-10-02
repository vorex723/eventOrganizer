package com.mazurek.eventOrganizer.city;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CitySchemaMigrationIntegrationTest {
    @Autowired private DataSource dataSource;

    private DataSource isolatedDataSource() {
        var hikari = (HikariDataSource) dataSource;
        return new DriverManagerDataSource(hikari.getJdbcUrl(), hikari.getUsername(), hikari.getPassword());
    }

    private Flyway migration(DataSource migrationDataSource, String schema, String target) {
        var config = Flyway.configure().dataSource(migrationDataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").cleanDisabled(false);
        if (target != null) config.target(target);
        return config.load();
    }

    @Test
    void freshSchemaSupportsSameNameCitiesButRejectsDuplicateExternalIdentifiers() throws Exception {
        String schema = "city_migration_" + UUID.randomUUID().toString().replace("-", "");
        DataSource migrationDataSource = isolatedDataSource();
        Flyway flyway = migration(migrationDataSource, schema, null);
        try {
            flyway.migrate();
            try (Connection connection = migrationDataSource.getConnection()) {
                connection.setSchema(schema);
                insert(connection, "test:first", "GB");
                insert(connection, "test:second", "US");
                try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT count(*) FROM cities WHERE name = 'Cambridge'")) {
                    result.next();
                    assertThat(result.getInt(1)).isEqualTo(2);
                }
                assertThatThrownBy(() -> insert(connection, "test:first", "PL")).isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> insert(connection, "test:invalid-country", "1X")).isInstanceOf(SQLException.class);
            }
        } finally {
            flyway.clean();
        }
    }

    @Test
    void populatedLegacySchemaIsRejectedWithoutInventingProviderData() throws Exception {
        String schema = "city_legacy_" + UUID.randomUUID().toString().replace("-", "");
        DataSource migrationDataSource = isolatedDataSource();
        Flyway flyway = migration(migrationDataSource, schema, null);
        try {
            migration(migrationDataSource, schema, "1.27").migrate();
            try (Connection connection = migrationDataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO cities (id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'warsaw')");
                }
            }
            assertThatThrownBy(flyway::migrate).hasStackTraceContaining("City refactor requires an empty database");
        } finally {
            flyway.clean();
        }
    }

    private void insert(Connection connection, String externalId, String countryCode) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO cities (id, external_id, name, country_code, latitude, longitude) VALUES (?, ?, 'Cambridge', ?, 52, 0)")) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, externalId);
            statement.setString(3, countryCode);
            statement.executeUpdate();
        }
    }
}
