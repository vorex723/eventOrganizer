package com.mazurek.eventOrganizer.testSupport.database;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Properties;

import static com.mazurek.eventOrganizer.testData.TestConstants.DatabaseConstants.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("TestDatabaseSafety unit tests:")
class TestDatabaseSafetyUnitTest {
    @Nested
    @DisplayName("Connection target tests:")
    class TargetTests {
        @ParameterizedTest(name = "Allowed target: {0}")
        @ValueSource(strings = {
                "jdbc:postgresql://localhost:5432/event_organizer_test",
                "jdbc:postgresql://127.0.0.1:55432/event_organizer_test",
                "jdbc:postgresql://test-db.internal/event_organizer_test",
                "jdbc:postgresql://[::1]:5432/event_organizer_test",
                "jdbc:postgresql://db.internal:6543/event_organizer_test?currentSchema=owned_test_123&sslmode=require&connectTimeout=5&socketTimeout=10&ApplicationName=tests"
        })
        void whenTargetIsDedicatedShouldAllowDifferentHostsPortsAndOwnedSchemas(String url) {
            assertThatCode(() -> TestDatabaseSafety.requireSafeTarget(url, TEST_DATABASE_USERNAME))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "Rejected target: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {
                "jdbc:postgresql://localhost:5432/event_organizer",
                "jdbc:postgresql://localhost:5432/postgres",
                "jdbc:postgresql:event_organizer_test",
                "jdbc:h2:mem:event_organizer_test",
                "jdbc:postgresql:///event_organizer_test",
                "jdbc:postgresql://host1,host2/event_organizer_test",
                "jdbc:postgresql://user:secret@localhost/event_organizer_test",
                "jdbc:postgresql://localhost/event_organizer_test#fragment",
                "jdbc:postgresql://localhost:0/event_organizer_test",
                "jdbc:postgresql://localhost:70000/event_organizer_test",
                "jdbc:postgresql://localhost:/event_organizer_test",
                "jdbc:postgresql://localhost/%65vent_organizer_test",
                "jdbc:postgresql://localhost/event_organizer_test/",
                "jdbc:postgresql://local host/event_organizer_test"
        })
        void whenTargetIsWrongOrAmbiguousShouldRejectWithoutConnecting(String url) {
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeTarget(url, TEST_DATABASE_USERNAME))
                    .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Unsafe test database:");
        }

        @ParameterizedTest(name = "Rejected login: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"postgres", "devuser", "eventorganizer_test ", "EVENTORGANIZER_TEST"})
        void whenLoginIsNotDedicatedShouldReject(String username) {
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeTarget(TEST_DATABASE_URL, username))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("dedicated");
        }

        @ParameterizedTest(name = "Rejected option: {0}")
        @ValueSource(strings = {
                "user=postgres", "password=secret", "options=-c%20role%3Dpostgres",
                "service=production", "socketFactory=custom.Factory", "currentSchema=public,private",
                "currentSchema=pg_catalog", "currentSchema=public&currentSchema=other",
                "currentSchema=", "currentSchema=public&", "currentSchema",
                "currentSchema=bad%ZZ", "currentSchema=public%3BDROP%20SCHEMA%20public",
                "%75ser=postgres", "connectTimeout=0", "socketTimeout=-1",
                "connectTimeout=2147483648", "sslmode=invalid"
        })
        void whenOptionsCanOverrideIdentityOrAreMalformedShouldReject(String options) {
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeTarget(TEST_DATABASE_URL + "?" + options,
                    TEST_DATABASE_USERNAME)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void whenUrlContainsCredentialsShouldNotEchoSecretsOrAttachParsingException() {
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeTarget(
                    "jdbc:postgresql://user:very-private-secret@bad host/event_organizer_test", TEST_DATABASE_USERNAME))
                    .hasMessageNotContaining("very-private-secret").hasNoCause();
        }
    }

    @Nested
    @DisplayName("Schema tests:")
    class SchemaTests {
        @ParameterizedTest(name = "Allowed schema: {0}")
        @ValueSource(strings = {"public", "baseline_contract_123", "baseline_registration_123", "local_seed_123"})
        void whenSchemaIsSingleNonSystemIdentifierShouldAllow(String schema) {
            assertThatCode(() -> TestDatabaseSafety.requireSafeSchema(schema)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "Rejected schema: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"pg_catalog", "pg_temp", "information_schema", "public,other", "Public",
                "public; DROP SCHEMA other", "\"public\"", " public", "a" +
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
        void whenSchemaIsSystemOrUnsafeIdentifierShouldReject(String schema) {
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeSchema(schema))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Actual connection identity tests:")
    class ConnectionIdentityTests {
        @Test
        void whenHikariHasPreVerificationSqlHookShouldRejectBeforeOpeningConnection() throws Exception {
            try (var source = spy(new HikariDataSource())) {
                source.setJdbcUrl(TEST_DATABASE_URL);
                source.setUsername(TEST_DATABASE_USERNAME);
                source.setConnectionInitSql("DELETE FROM users");
                assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                        .hasMessageContaining("pre-verification SQL hooks");
                verify(source, never()).getConnection();
            }
        }

        @Test
        void whenDriverPropertiesCanOverrideLoginShouldRejectBeforeOpeningConnection() throws Exception {
            var source = source();
            Properties properties = new Properties();
            properties.setProperty("user", "postgres");
            source.setConnectionProperties(properties);
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessageContaining("credential/session overrides");
            verify(source, never()).getConnection();
        }

        @Test
        void whenCleanupAlreadyHasTransactionShouldReuseConnectionWithoutClosingIt() throws Exception {
            var source = source();
            Connection connection = mock(Connection.class);
            identity(connection);
            TransactionSynchronizationManager.bindResource(source, new ConnectionHolder(connection));
            try {
                assertThatCode(() -> TestDatabaseSafety.requireSafeDataSource(source)).doesNotThrowAnyException();
                verify(source, never()).getConnection();
                verify(connection, never()).close();
            } finally {
                TransactionSynchronizationManager.unbindResource(source);
            }
        }

        @Test
        void whenIdentityQueryFailsShouldStopAndReleaseConnection() throws Exception {
            var source = source();
            Connection connection = mock(Connection.class);
            when(connection.createStatement()).thenThrow(new SQLException("identity unavailable"));
            doReturn(connection).when(source).getConnection();
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessage("Cannot verify the dedicated test database identity")
                    .hasCauseInstanceOf(SQLException.class);
            verify(connection).close();
        }

        @Test
        void whenIdentityQueryReturnsNoRoleShouldStopAndReleaseConnection() throws Exception {
            var source = source();
            Connection connection = mock(Connection.class);
            ResultSet identity = identity(connection);
            when(identity.next()).thenReturn(false);
            doReturn(connection).when(source).getConnection();
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessageStartingWith("Unsafe test database:");
            verify(connection).close();
        }

        @Test
        void whenDataSourceTypeCannotBeInspectedShouldRejectBeforeConnection() {
            DataSource source = mock(DataSource.class);
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessageContaining("unsupported DataSource");
            verifyNoInteractions(source);
        }

        @Test
        void whenActualSourceOverridesDeclaredTargetShouldRejectBeforeConnection() throws Exception {
            var source = spy(new DriverManagerDataSource(
                    "jdbc:postgresql://foreign.invalid/event_organizer", TEST_DATABASE_USERNAME, "unused"));
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessageStartingWith("Unsafe test database:");
            verify(source, never()).getConnection();
        }

        @Test
        void whenRoleIsDedicatedOwnerWithoutPrivilegesShouldAcceptAndReleaseConnection() throws Exception {
            var source = source();
            Connection connection = mock(Connection.class);
            ResultSet identity = identity(connection);
            doReturn(connection).when(source).getConnection();

            assertThatCode(() -> TestDatabaseSafety.requireSafeDataSource(source)).doesNotThrowAnyException();

            verify(identity).getBoolean("owns_database");
            verify(connection).close();
        }

        @ParameterizedTest(name = "Rejected privilege: {0}")
        @ValueSource(strings = {"rolsuper", "rolcreatedb", "rolcreaterole", "rolreplication", "rolbypassrls", "has_memberships"})
        void whenRoleHasAdministrativePrivilegeOrMembershipShouldRejectAndReleaseConnection(String privilege) throws Exception {
            var source = source();
            Connection connection = mock(Connection.class);
            ResultSet identity = identity(connection);
            when(identity.getBoolean(privilege)).thenReturn(true);
            doReturn(connection).when(source).getConnection();

            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessageStartingWith("Unsafe test database:");
            verify(connection).close();
        }

        @ParameterizedTest(name = "Rejected server identity: {0}")
        @ValueSource(strings = {"database_name", "role_name", "login_name"})
        void whenServerIdentityDiffersShouldReject(String field) throws Exception {
            var source = source();
            Connection connection = mock(Connection.class);
            ResultSet identity = identity(connection);
            when(identity.getString(field)).thenReturn("not-the-test-target");
            doReturn(connection).when(source).getConnection();

            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessageStartingWith("Unsafe test database:");
            verify(connection).close();
        }

        @Test
        void whenRoleDoesNotOwnTestDatabaseShouldReject() throws Exception {
            var source = source();
            Connection connection = mock(Connection.class);
            ResultSet identity = identity(connection);
            when(identity.getBoolean("owns_database")).thenReturn(false);
            doReturn(connection).when(source).getConnection();
            assertThatThrownBy(() -> TestDatabaseSafety.requireSafeDataSource(source))
                    .hasMessageStartingWith("Unsafe test database:");
            verify(connection).close();
        }

        private DriverManagerDataSource source() {
            return spy(new DriverManagerDataSource(TEST_DATABASE_URL, TEST_DATABASE_USERNAME, "unused"));
        }

        private ResultSet identity(Connection connection) throws Exception {
            Statement statement = mock(Statement.class);
            ResultSet identity = mock(ResultSet.class);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(identity);
            when(identity.next()).thenReturn(true);
            when(identity.getString("database_name")).thenReturn(TEST_DATABASE);
            when(identity.getString("role_name")).thenReturn(TEST_DATABASE_USERNAME);
            when(identity.getString("login_name")).thenReturn(TEST_DATABASE_USERNAME);
            when(identity.getBoolean("owns_database")).thenReturn(true);
            return identity;
        }
    }
}
