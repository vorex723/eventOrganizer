package com.mazurek.eventOrganizer.common;

import com.zaxxer.hikari.HikariDataSource;
import com.mazurek.eventOrganizer.testSupport.database.TestDatabaseSafety;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BaselineSchemaIntegrationTest contracts:")
class BaselineSchemaIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @TempDir Path migrationDirectory;

    private String schema;
    private Flyway flyway;
    private Connection connection;

    private static final String CITY = "00000000-0000-0000-0000-000000000001";
    private static final String USER = "00000000-0000-0000-0000-000000000002";
    private static final String OTHER_USER = "00000000-0000-0000-0000-000000000003";
    private static final String EVENT = "00000000-0000-0000-0000-000000000004";
    private static final String THREAD = "00000000-0000-0000-0000-000000000005";
    private static final String CONVERSATION = "00000000-0000-0000-0000-000000000006";
    private static final String NOTIFICATION = "00000000-0000-0000-0000-000000000007";
    private static final String DELIVERY = "00000000-0000-0000-0000-000000000008";
    private static final String DEVICE = "00000000-0000-0000-0000-000000000009";
    private static final String TAG = "00000000-0000-0000-0000-000000000010";
    private static final String EXTRA = "00000000-0000-0000-0000-000000000099";

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        schema = "baseline_contract_" + UUID.randomUUID().toString().replace("-", "");
        var resource = new ClassPathResource("db/migration/V1__baseline.sql");
        try (var source = resource.getInputStream()) {
            Files.copy(source, migrationDirectory.resolve(resource.getFilename()));
        }
        var original = (HikariDataSource) dataSource;
        var isolated = new DriverManagerDataSource(original.getJdbcUrl(), original.getUsername(), original.getPassword());
        TestDatabaseSafety.requireSafeDataSource(isolated);
        TestDatabaseSafety.requireSafeSchema(schema);
        flyway = Flyway.configure().dataSource(isolated).schemas(schema).defaultSchema(schema)
                .locations("filesystem:" + migrationDirectory.toAbsolutePath()).cleanDisabled(false).load();
        flyway.migrate();
        connection = isolated.getConnection();
        connection.setSchema(schema);
    }

    @AfterEach
    void removeOnlyOwnedSchema() throws Exception {
        try {
            if (connection != null) connection.close();
        } finally {
            if (flyway != null) {
                TestDatabaseSafety.requireSafeDataSource(flyway.getConfiguration().getDataSource());
                TestDatabaseSafety.requireSafeSchema(schema);
                flyway.clean();
            }
        }
    }

    @Test
    void whenBaselineStartsFreshShouldProvisionTablesRolesAndSequence() throws Exception {
        assertThat(strings("SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema()"))
                .containsExactlyInAnyOrder("cities", "roles", "users", "user_roles", "tags", "events", "event_user",
                        "event_tag", "threads", "thread_replies", "files", "conversations", "conversation_participant",
                        "direct_conversation_pair", "messages", "activation_tokens", "password_reset_tokens",
                        "email_change_tokens", "refresh_tokens", "auth_email_deliveries", "notifications",
                        "notification_deliveries", "notification_devices", "notification_preferences", "flyway_schema_history");
        assertThat(strings("SELECT name FROM roles"))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");
        assertThat(number("SELECT count(*) FROM users")).isZero();
        assertThat(number("SELECT count(*) FROM cities")).isZero();
        assertThat(number("SELECT increment_by FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'message_id_seq'"))
                .isEqualTo(50);
        assertThat(strings("SELECT indexname FROM pg_indexes WHERE schemaname = current_schema()"))
                .contains("uq_tags_normalized_name", "idx_events_owner_start_id", "idx_events_city_start_id",
                        "idx_event_user_user_event", "idx_files_event_upload_id", "idx_threads_event_last_activity_id",
                        "idx_thread_replies_thread_date_id", "idx_message_conversation_created",
                        "idx_conversation_participant_user", "idx_event_tag_tag_event",
                        "idx_refresh_tokens_user_device_type", "idx_users_lower_email",
                        "idx_threads_user", "idx_thread_replies_user", "idx_files_user", "idx_messages_sender",
                        "idx_direct_conversation_pair_second_user",
                        "idx_refresh_tokens_family_id", "idx_auth_email_deliveries_processable",
                        "idx_auth_email_deliveries_user_type_created", "idx_notifications_recipient_created_at",
                        "idx_notifications_recipient_unread", "idx_notifications_created_at",
                        "idx_notification_deliveries_processable", "idx_notification_deliveries_claim_token",
                        "idx_notification_deliveries_target_device", "idx_notification_deliveries_dead_created_at",
                        "idx_notification_devices_user_platform")
                .doesNotContain("idx_message_conversation_id", "idx_conversation_participant_conversation",
                        "idx_notification_preferences_user");
        assertThat(strings("SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'thread_replies'"))
                .contains("edit_count").doesNotContain("edit_counter", "replier_name_at_creation");
        assertThat(strings("SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name IN ('activation_tokens','password_reset_tokens','email_change_tokens','refresh_tokens')"))
                .doesNotContain("token", "device_info");
        assertThat(strings("SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'cities'"))
                .containsExactlyInAnyOrder("id", "external_id", "name", "country_code", "admin_area",
                        "latitude", "longitude", "time_zone_id");
        assertThat(strings("SELECT table_name || '.' || column_name FROM information_schema.columns WHERE table_schema = current_schema() "
                + "AND ((table_name IN ('cities','events') AND column_name = 'time_zone_id') OR (table_name = 'users' AND column_name IN ('time_zone','city_id')))"))
                .containsExactlyInAnyOrder("cities.time_zone_id", "users.time_zone", "users.city_id");
        assertThat(strings("SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'refresh_tokens'"))
                .containsExactlyInAnyOrder("id", "token_hash", "family_id", "user_id", "device_type",
                        "created_at", "expiry_date", "last_used_at", "revoked");
    }

    @Test
    void whenInspectingBaselineIndexesShouldMatchQueryAndLookupContracts() throws Exception {
        Map<String, String> definitions = Map.ofEntries(
                Map.entry("idx_event_tag_tag_event", "(tag_id, event_id)"),
                Map.entry("idx_thread_replies_thread_date_id", "(thread_id, reply_date, id DESC)"),
                Map.entry("idx_message_conversation_created", "(conversation_id, sent_date DESC, id DESC)"),
                Map.entry("idx_notifications_recipient_created_at", "(recipient_id, created_at DESC, id DESC)"),
                Map.entry("idx_refresh_tokens_user_device_type", "(user_id, device_type)"),
                Map.entry("idx_users_lower_email", "(lower((email)::text))"),
                Map.entry("idx_threads_user", "(user_id)"),
                Map.entry("idx_thread_replies_user", "(user_id)"),
                Map.entry("idx_files_user", "(user_id)"),
                Map.entry("idx_messages_sender", "(sender_id)"),
                Map.entry("idx_direct_conversation_pair_second_user", "(second_user_id)"),
                Map.entry("uk_conversation_participant_conversation_user", "(conversation_id, user_id)"),
                Map.entry("uq_notification_preferences_user_resource_channel", "(user_id, resource_type, channel)"),
                Map.entry("users_email_key", "(email)")
        );
        for (var definition : definitions.entrySet()) {
            assertThat(strings("SELECT indexdef FROM pg_indexes WHERE schemaname = current_schema() AND indexname = '"
                    + definition.getKey() + "'"))
                    .as("Index definition for %s", definition.getKey())
                    .singleElement().asString().endsWith(" USING btree " + definition.getValue());
        }
        for (String uniqueIndex : List.of("uk_conversation_participant_conversation_user",
                "uq_notification_preferences_user_resource_channel", "users_email_key")) {
            assertThat(strings("SELECT indexdef FROM pg_indexes WHERE schemaname = current_schema() AND indexname = '"
                    + uniqueIndex + "'"))
                    .singleElement().asString().startsWith("CREATE UNIQUE INDEX ");
        }
    }

    @Test
    void whenInspectingEntityColumnsShouldMatchRequiredLengthsAndRejectNulls() throws Exception {
        seedGraph();
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(21);
        for (var entity : entityManagerFactory.getMetamodel().getEntities()) {
            Class<?> type = entity.getJavaType();
            String table = type.getAnnotation(Table.class).name();
            for (var field : type.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                if (column == null && join == null) continue;
                boolean required = column != null ? !column.nullable() : !join.nullable();
                String name = column != null ? column.name() : join.name();
                if (name.isEmpty()) name = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
                try (var query = connection.prepareStatement("""
                        SELECT is_nullable, character_maximum_length FROM information_schema.columns
                        WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?
                        """)) {
                    query.setString(1, table);
                    query.setString(2, name);
                    try (var result = query.executeQuery()) {
                        assertThat(result.next()).as("%s.%s exists", table, name).isTrue();
                        assertThat(result.getString(1)).as("%s.%s nullability", table, name).isEqualTo(required ? "NO" : "YES");
                        if (column != null && result.getObject(2) != null) {
                            assertThat(result.getInt(2)).as("%s.%s length", table, name).isEqualTo(column.length());
                        }
                    }
                }
                if (required) rejects("UPDATE " + table + " SET " + name + " = NULL", "23502");
            }
        }
        for (String table : List.of("user_roles", "event_user", "event_tag")) {
            assertThat(number("SELECT count(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = '"
                    + table + "' AND is_nullable = 'YES'")).isZero();
        }
    }

    @Test
    void whenBaselineValuesAreInvalidShouldRejectRows() throws Exception {
        seedGraph();
        for (String sql : List.of(
                "UPDATE tags SET name = E' \\t\\n'",
                "UPDATE events SET name = ''",
                "UPDATE events SET long_description = '   '",
                "UPDATE events SET attendee_count = -1",
                "UPDATE events SET max_attendees = 0",
                "UPDATE threads SET edit_count = -1",
                "UPDATE threads SET reply_count = -1",
                "UPDATE threads SET version = -1",
                "UPDATE thread_replies SET edit_count = -1",
                "UPDATE thread_replies SET version = -1",
                "UPDATE users SET security_version = -1",
                "UPDATE users SET notification_preferences_version = -1",
                "UPDATE notification_deliveries SET attempt_count = -1",
                "UPDATE auth_email_deliveries SET attempt_count = -1",
                "UPDATE cities SET latitude = 'NaN'::float8",
                "UPDATE cities SET longitude = 181",
                "UPDATE cities SET country_code = 'pl'",
                "UPDATE cities SET time_zone_id = ' '",
                "UPDATE conversations SET type = 'INVALID'",
                "UPDATE notification_deliveries SET channel = 'IN_APP'",
                "UPDATE notification_devices SET platform = 'INVALID'",
                "UPDATE notifications SET parent_resource_id = '" + EVENT + "'",
                "UPDATE direct_conversation_pair SET first_user_id = second_user_id")) {
            rejects(sql, "23514");
        }
        rejects("UPDATE events SET name = repeat('x', 256)", "22001");
        rejects("UPDATE threads SET content = repeat('x', 1001)", "22001");
        rejects("UPDATE thread_replies SET content = repeat('x', 1001)", "22001");
    }

    @Test
    void whenValuesConflictShouldEnforceNormalizedCompoundAndNamedUniqueness() throws Exception {
        seedGraph();
        rejects("INSERT INTO tags VALUES ('" + EXTRA + "', ' Music ')", "23505");
        assertThat(execute("INSERT INTO tags VALUES ('" + EXTRA + "', 'MUSIC') ON CONFLICT ((lower(btrim(name)))) DO NOTHING")).isZero();
        rejects("INSERT INTO cities SELECT '" + EXTRA + "', external_id, name, country_code, admin_area, latitude, longitude, time_zone_id FROM cities", "23505");
        assertThat(execute("INSERT INTO cities SELECT '" + EXTRA + "', 'other:city', name, country_code, admin_area, latitude, longitude, time_zone_id FROM cities")).isEqualTo(1);
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> execute("UPDATE users SET email = 'first@example.test' WHERE id = '" + OTHER_USER + "'"))
                .withMessageContaining("users_email_key");
        assertThatExceptionOfType(SQLException.class)
                .isThrownBy(() -> execute("INSERT INTO email_change_tokens(token_hash,pending_email,user_id,expiration_date) VALUES (repeat('b',64),'pending@example.test','" + OTHER_USER + "',now())"))
                .withMessageContaining("email_change_tokens_pending_email_key");
        rejects("INSERT INTO conversation_participant(conversation_id,user_id,user_name_at_join,joined_at) VALUES ('" + CONVERSATION + "','" + USER + "','Duplicate',now())", "23505");
        rejects("INSERT INTO notification_devices(id,user_id,platform,firebase_installation_id,created_at) VALUES ('" + EXTRA + "','" + USER + "','WEB','installation',now())", "23505");
        rejects("INSERT INTO notification_preferences(id,user_id,resource_type,channel,enabled) VALUES ('" + EXTRA + "','" + OTHER_USER + "','EVENT','EMAIL',false)", "23505");
        rejects("INSERT INTO notification_deliveries(id,notification_id,channel,target_key,status,created_at) VALUES ('" + EXTRA + "','" + NOTIFICATION + "','EMAIL','email:second@example.test','PENDING',now())", "23505");
        for (String table : List.of("activation_tokens", "password_reset_tokens", "refresh_tokens")) {
            if (table.equals("refresh_tokens")) {
                rejects("INSERT INTO refresh_tokens(token_hash,family_id,user_id,expiry_date,device_type,created_at,last_used_at) SELECT token_hash,family_id,'" + OTHER_USER + "',expiry_date,device_type,created_at,last_used_at FROM refresh_tokens", "23505");
            } else {
                rejects("INSERT INTO " + table + "(token_hash,user_id,expiration_date) SELECT token_hash,'" + OTHER_USER + "',expiration_date FROM " + table, "23505");
            }
        }
    }

    @Test
    void whenParentIsMissingShouldRejectForeignKeyReferences() throws Exception {
        seedGraph();
        for (String target : List.of("users.city_id", "events.city_id", "events.user_id", "threads.event_id",
                "threads.user_id", "thread_replies.thread_id", "thread_replies.user_id", "files.event_id",
                "files.user_id", "conversation_participant.conversation_id", "conversation_participant.user_id",
                "messages.conversation_id", "messages.sender_id", "activation_tokens.user_id",
                "password_reset_tokens.user_id", "email_change_tokens.user_id", "refresh_tokens.user_id",
                "auth_email_deliveries.user_id", "notifications.recipient_id", "notification_deliveries.notification_id",
                "notification_devices.user_id", "notification_preferences.user_id", "event_user.event_id",
                "event_user.user_id", "event_tag.event_id", "event_tag.tag_id", "user_roles.user_id",
                "direct_conversation_pair.conversation_id", "direct_conversation_pair.second_user_id")) {
            String[] parts = target.split("\\.");
            rejects("UPDATE " + parts[0] + " SET " + parts[1] + " = 'ffffffff-ffff-ffff-ffff-ffffffffffff'", "23503");
        }
        rejects("UPDATE user_roles SET role_id = 999999", "23503");
        // The missing first user must also keep the pair-order CHECK valid.
        rejects("UPDATE direct_conversation_pair SET first_user_id = '00000000-0000-0000-0000-000000000000'", "23503");
    }

    @Test
    void whenDefaultsAndLegalNullsAreUsedShouldPreserveDeliverySnapshots() throws Exception {
        seedGraph();
        assertThat(number("SELECT attendee_count FROM events")).isZero();
        assertThat(strings("SELECT max_attendees::text FROM events")).containsExactly((String) null);
        assertThat(number("SELECT count(*) FROM users WHERE activated = false AND banned = false AND security_version = 0 AND notification_preferences_version = 0")).isEqualTo(2);
        assertThat(number("SELECT count(*) FROM threads WHERE edit_count = 0 AND reply_count = 0 AND version = 0")).isEqualTo(1);
        assertThat(number("SELECT count(*) FROM thread_replies WHERE edit_count = 0 AND version = 0")).isEqualTo(1);
        assertThat(number("SELECT count(*) FROM refresh_tokens WHERE revoked = false")).isEqualTo(1);
        assertThat(number("SELECT attempt_count FROM auth_email_deliveries")).isZero();
        assertThat(number("SELECT attempt_count FROM notification_deliveries")).isZero();
        assertThat(number("SELECT count(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'events' AND column_name = 'max_attendees' AND column_default IS NULL")).isEqualTo(1);
        execute("UPDATE events SET max_attendees = 100000");
        execute("UPDATE events SET max_attendees = NULL");
        execute("UPDATE notification_deliveries SET target_device_id = '" + DEVICE + "', target_email = NULL, status = 'SENT', sent_at = now()");
        execute("DELETE FROM notification_devices");
        assertThat(strings("SELECT target_device_id::text FROM notification_deliveries")).containsExactly(DEVICE);
        assertThat(number("SELECT count(*) FROM conversations WHERE name IS NULL")).isEqualTo(1);
        assertThat(number("SELECT count(*) FROM notifications WHERE read_at IS NULL AND parent_resource_id IS NULL AND parent_resource_type IS NULL")).isEqualTo(1);
    }

    @Test
    void whenUserIsDeletedShouldAnonymizeContentAndCascadePrivateData() throws Exception {
        seedGraph();
        execute("DELETE FROM users WHERE id = '" + USER + "'");
        for (String table : List.of("events", "threads", "thread_replies", "files")) {
            assertThat(number("SELECT count(*) FROM " + table + " WHERE user_id IS NULL")).as(table).isEqualTo(1);
        }
        assertThat(strings("SELECT sender_name_at_creation FROM messages WHERE sender_id IS NULL")).containsExactly("First User");
        assertThat(strings("SELECT user_name_at_join FROM conversation_participant WHERE user_id IS NULL")).containsExactly("First User");
        for (String table : List.of("activation_tokens", "password_reset_tokens", "email_change_tokens", "refresh_tokens", "auth_email_deliveries", "direct_conversation_pair", "user_roles")) {
            assertThat(number("SELECT count(*) FROM " + table)).as(table).isZero();
        }
        // Standard UNIQUE must allow more than one anonymized participant.
        execute("INSERT INTO conversation_participant(conversation_id,user_name_at_join,joined_at) VALUES ('" + CONVERSATION + "','Deleted user',now())");
        assertThat(number("SELECT count(*) FROM conversation_participant WHERE user_id IS NULL")).isEqualTo(2);
        execute("DELETE FROM users WHERE id = '" + OTHER_USER + "'");
        assertThat(number("SELECT count(*) FROM notifications")).isZero();
        assertThat(number("SELECT count(*) FROM notification_deliveries")).isZero();
        assertThat(number("SELECT count(*) FROM notification_devices")).isZero();
        assertThat(number("SELECT count(*) FROM notification_preferences")).isZero();
    }

    @Test
    void whenCurrentBaselineRunsAgainShouldPreserveRowsAndReferenceRoles() throws Exception {
        seedGraph();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(number("SELECT count(*) FROM roles")).isEqualTo(3);
        assertThat(number("SELECT count(*) FROM events")).isEqualTo(1);
        assertThat(number("SELECT count(*) FROM messages")).isEqualTo(1);
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE success = true AND type = 'SQL'")).isEqualTo(1);
    }

    private void seedGraph() throws SQLException {
        execute("INSERT INTO cities VALUES ('" + CITY + "','test:city','Warsaw','PL',NULL,52,21,'Europe/Warsaw')");
        for (String userId : List.of(USER, OTHER_USER)) {
            String email = userId.equals(USER) ? "first@example.test" : "second@example.test";
            execute("INSERT INTO users(id,first_name,last_name,email,password,city_id,created_at,time_zone,last_credentials_change_time) VALUES ('" + userId + "','First','User','" + email + "','hash','" + CITY + "',now(),'Europe/Warsaw',now())");
        }
        execute("INSERT INTO user_roles SELECT '" + USER + "',id FROM roles WHERE name = 'ROLE_USER'");
        execute("INSERT INTO tags VALUES ('" + TAG + "','music')");
        execute("INSERT INTO events(id,name,short_description,long_description,create_date,last_update,event_start_date,city_id,exact_address,user_id) VALUES ('" + EVENT + "','Example event','Short description','Long description',now(),now(),now(),'" + CITY + "','Address','" + USER + "')");
        execute("INSERT INTO event_user VALUES ('" + EVENT + "','" + USER + "')");
        execute("INSERT INTO event_tag VALUES ('" + EVENT + "','" + TAG + "')");
        execute("INSERT INTO threads(id,event_id,user_id,name,content,create_date,last_update,last_activity) VALUES ('" + THREAD + "','" + EVENT + "','" + USER + "','Example thread','Thread content',now(),now(),now())");
        execute("INSERT INTO thread_replies(id,thread_id,user_id,content,reply_date,last_update) VALUES ('" + EXTRA + "','" + THREAD + "','" + USER + "','Reply content',now(),now())");
        execute("INSERT INTO files(id,event_id,user_id,user_file_name,original_file_name,content_type,content,upload_date_time) VALUES ('" + EXTRA + "','" + EVENT + "','" + USER + "','Example file','file.txt','text/plain',decode('01','hex'),now())");
        execute("INSERT INTO conversations(id,type,created_at,last_active_at) VALUES ('" + CONVERSATION + "','DIRECT',now(),now())");
        execute("INSERT INTO conversation_participant(conversation_id,user_id,user_name_at_join,joined_at) VALUES ('" + CONVERSATION + "','" + USER + "','First User',now())");
        execute("INSERT INTO direct_conversation_pair(conversation_id,first_user_id,second_user_id) VALUES ('" + CONVERSATION + "','" + USER + "','" + OTHER_USER + "')");
        execute("INSERT INTO messages VALUES (1,'" + CONVERSATION + "',now(),'" + USER + "','First User','default','ciphertext')");
        for (String table : List.of("activation_tokens", "password_reset_tokens")) {
            execute("INSERT INTO " + table + "(token_hash,user_id,expiration_date) VALUES (repeat('a',64),'" + USER + "',now())");
        }
        execute("INSERT INTO email_change_tokens(token_hash,pending_email,user_id,expiration_date) VALUES (repeat('a',64),'pending@example.test','" + USER + "',now())");
        execute("INSERT INTO refresh_tokens(token_hash,family_id,user_id,expiry_date,device_type,created_at,last_used_at) VALUES (repeat('a',64),'" + EXTRA + "','" + USER + "',now(),'WEB',now(),now())");
        execute("INSERT INTO auth_email_deliveries(id,user_id,recipient_email,type,encrypted_token,status,created_at) VALUES ('" + EXTRA + "','" + USER + "','first@example.test','ACCOUNT_ACTIVATION','ciphertext','PENDING',now())");
        execute("INSERT INTO notifications(id,recipient_id,title,body,resource_type,resource_id,created_at) VALUES ('" + NOTIFICATION + "','" + OTHER_USER + "','Title','Body','EVENT','" + EVENT + "',now())");
        execute("INSERT INTO notification_deliveries(id,notification_id,channel,target_key,target_email,status,created_at) VALUES ('" + DELIVERY + "','" + NOTIFICATION + "','EMAIL','email:second@example.test','second@example.test','PENDING',now())");
        execute("INSERT INTO notification_devices(id,user_id,platform,firebase_installation_id,created_at) VALUES ('" + DEVICE + "','" + OTHER_USER + "','WEB','installation',now())");
        execute("INSERT INTO notification_preferences VALUES ('" + EXTRA + "','" + OTHER_USER + "','EVENT','EMAIL',false)");
    }

    private int execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) { return statement.executeUpdate(sql); }
    }

    private void rejects(String sql, String state) {
        assertThatExceptionOfType(SQLException.class).as(sql).isThrownBy(() -> execute(sql))
                .satisfies(exception -> assertThat(exception.getSQLState()).isEqualTo(state));
    }

    private long number(String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private List<String> strings(String sql) throws SQLException {
        var values = new ArrayList<String>();
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            while (result.next()) values.add(result.getString(1));
        }
        return values;
    }
}
