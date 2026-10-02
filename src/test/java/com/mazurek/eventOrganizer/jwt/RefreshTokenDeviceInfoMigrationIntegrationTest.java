package com.mazurek.eventOrganizer.jwt;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenDeviceInfoMigrationIntegrationTest {

    private static final String TOKEN_DATA_QUERY = """
            SELECT id, token_hash, family_id, user_id, device_type,
                   created_at, expiry_date, last_used_at, revoked
            FROM refresh_tokens ORDER BY id
            """;

    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void migrationRemovesDeviceInfoOnFreshAndExistingSchemasWithoutChangingTokenData(boolean upgrade)
            throws Exception {
        String schema = "refresh_token_migration_" + UUID.randomUUID().toString().replace("-", "");
        HikariDataSource source = (HikariDataSource) dataSource;
        DataSource migrationDataSource = new DriverManagerDataSource(
                source.getJdbcUrl(), source.getUsername(), source.getPassword());
        try (Connection connection = migrationDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }

        try {
            List<Map<String, Object>> tokensBefore = List.of();
            if (upgrade) {
                migrate(migrationDataSource, schema, "1.26");
                try (Connection connection = migrationDataSource.getConnection()) {
                    connection.setSchema(schema);
                    JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                    assertThat(columnNames(jdbc)).contains("device_info");
                    seedLegacyTokens(jdbc);
                    tokensBefore = jdbc.queryForList(TOKEN_DATA_QUERY);
                    assertThat(tokensBefore).hasSize(2);
                }
            }

            migrate(migrationDataSource, schema, "1.27");

            try (Connection connection = migrationDataSource.getConnection()) {
                connection.setSchema(schema);
                JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                assertThat(columnNames(jdbc)).containsExactlyInAnyOrder(
                        "id", "token_hash", "family_id", "user_id", "device_type",
                        "created_at", "expiry_date", "last_used_at", "revoked");
                assertThat(jdbc.queryForList(TOKEN_DATA_QUERY)).containsExactlyElementsOf(tokensBefore);
            }
        } finally {
            try (Connection connection = migrationDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private void migrate(DataSource migrationDataSource, String schema, String target) {
        var configuration = Flyway.configure()
                .dataSource(migrationDataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        configuration.load().migrate();
    }

    private List<String> columnNames(JdbcTemplate jdbc) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'refresh_tokens'
                """, String.class);
    }

    private void seedLegacyTokens(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO cities (id, name)
                VALUES ('00000000-0000-0000-0000-000000000001', 'warsaw')
                """);
        jdbc.update("""
                INSERT INTO users (id, activated, banned, created_at, last_credentials_change_time,
                                   city_id, email, first_name, last_name, password, time_zone)
                VALUES ('00000000-0000-0000-0000-000000000002', true, false, now(), now(),
                        '00000000-0000-0000-0000-000000000001', 'migration.user@example.com',
                        'Migration', 'User', 'unused', 'Europe/Warsaw')
                """);
        jdbc.update("""
                INSERT INTO refresh_tokens (token_hash, family_id, user_id, device_type, device_info,
                                            created_at, expiry_date, last_used_at, revoked)
                VALUES (repeat('a', 64), '00000000-0000-0000-0000-000000000003',
                        '00000000-0000-0000-0000-000000000002', 'WEB', 'Legacy browser',
                        now() - interval '1 day', now() + interval '1 day', now(), false),
                       (repeat('b', 64), '00000000-0000-0000-0000-000000000004',
                        '00000000-0000-0000-0000-000000000002', 'MOBILE_ANDROID', 'Legacy phone',
                        now() - interval '2 days', now() - interval '1 day', now() - interval '1 day', true)
                """);
    }
}
