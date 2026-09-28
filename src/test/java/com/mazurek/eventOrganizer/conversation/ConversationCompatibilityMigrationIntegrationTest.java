package com.mazurek.eventOrganizer.conversation;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConversationCompatibilityMigrationIntegrationTest {

    @Autowired private DataSource dataSource;

    @Test
    void v119BackfillsMessageAndParticipantSnapshotsAndKeepsThemAfterUserRemoval() throws Exception {
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        DataSource migrationDataSource = isolatedMigrationDataSource();
        try (Connection connection = migrationDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }

        try {
            migrate(migrationDataSource, schema, "1.18");
            seedLegacyConversation(migrationDataSource, schema);
            migrate(migrationDataSource, schema, null);

            try (Connection connection = schemaConnection(migrationDataSource, schema)) {
                assertThat(queryForString(connection, "SELECT encryption_key_id FROM messages WHERE id = 1"))
                        .isEqualTo("default");
                assertThat(queryForString(connection, "SELECT sender_name_at_creation FROM messages WHERE id = 1"))
                        .isEqualTo("Legacy Sender");
                assertThat(queryForString(connection, "SELECT sender_name_at_creation FROM messages WHERE id = 2"))
                        .isEqualTo("Deleted user");
                assertThat(queryForString(connection, "SELECT user_name_at_join FROM conversation_participant WHERE id = 1"))
                        .isEqualTo("Legacy Sender");

                execute(connection, "DELETE FROM users WHERE id = '00000000-0000-0000-0000-000000000020'");

                assertThat(queryForInt(connection, "SELECT COUNT(*) FROM messages WHERE id = 1 AND sender_id IS NULL"))
                        .isEqualTo(1);
                assertThat(queryForString(connection, "SELECT sender_name_at_creation FROM messages WHERE id = 1"))
                        .isEqualTo("Legacy Sender");
                assertThat(queryForInt(connection, "SELECT COUNT(*) FROM conversation_participant WHERE id = 1 AND user_id IS NULL"))
                        .isEqualTo(1);
                assertThat(queryForString(connection, "SELECT user_name_at_join FROM conversation_participant WHERE id = 1"))
                        .isEqualTo("Legacy Sender");
            }
        } finally {
            try (Connection connection = migrationDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private DataSource isolatedMigrationDataSource() {
        if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
            throw new IllegalStateException("Expected the test DataSource to be Hikari-backed.");
        }
        return new DriverManagerDataSource(
                hikariDataSource.getJdbcUrl(),
                hikariDataSource.getUsername(),
                hikariDataSource.getPassword()
        );
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

    private void seedLegacyConversation(DataSource migrationDataSource, String schema) throws Exception {
        try (Connection connection = schemaConnection(migrationDataSource, schema)) {
            execute(connection, "INSERT INTO cities (id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'Warsaw')");
            execute(connection, """
                    INSERT INTO users (id, activated, banned, created_at, last_credentials_change_time, city_id,
                                       email, first_name, last_name, password, time_zone)
                    VALUES ('00000000-0000-0000-0000-000000000020', true, false, now(), now(),
                            '00000000-0000-0000-0000-000000000001', 'legacy.sender@example.com',
                            'Legacy', 'Sender', 'unused', 'Europe/Warsaw')
                    """);
            execute(connection, """
                    INSERT INTO conversations (id, type, created_at, last_active_at)
                    VALUES ('00000000-0000-0000-0000-000000000030', 'DIRECT', now(), now())
                    """);
            execute(connection, """
                    INSERT INTO conversation_participant (id, conversation_id, user_id, joined_at)
                    VALUES (1, '00000000-0000-0000-0000-000000000030',
                            '00000000-0000-0000-0000-000000000020', now())
                    """);
            execute(connection, """
                    INSERT INTO messages (id, conversation_id, sender_id, sent_date, content)
                    VALUES (1, '00000000-0000-0000-0000-000000000030',
                            '00000000-0000-0000-0000-000000000020', now(), 'legacy-ciphertext')
                    """);
            execute(connection, """
                    INSERT INTO messages (id, conversation_id, sender_id, sent_date, content)
                    VALUES (2, '00000000-0000-0000-0000-000000000030', null, now(), 'legacy-orphan-ciphertext')
                    """);
        }
    }

    private Connection schemaConnection(DataSource migrationDataSource, String schema) throws Exception {
        Connection connection = migrationDataSource.getConnection();
        connection.setSchema(schema);
        return connection;
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private int queryForInt(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String queryForString(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
