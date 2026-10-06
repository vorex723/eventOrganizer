package com.mazurek.eventOrganizer.testSupport.database;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Test-only, fail-closed protection. This is not a production database security boundary. */
public final class TestDatabaseSafety {
    private static final String DATABASE = "event_organizer_test";
    private static final String USERNAME = "eventorganizer_test";
    private static final Set<String> CONNECTION_OPTIONS = Set.of(
            "currentSchema", "sslmode", "connectTimeout", "socketTimeout", "ApplicationName");
    private static final String IDENTITY_QUERY = """
            SELECT current_database() AS database_name, current_user AS role_name,
                   session_user AS login_name, r.rolsuper, r.rolcreatedb, r.rolcreaterole,
                   r.rolreplication, r.rolbypassrls, d.datdba = r.oid AS owns_database,
                   EXISTS (SELECT 1 FROM pg_auth_members m WHERE m.member = r.oid) AS has_memberships
            FROM pg_roles r JOIN pg_database d ON d.datname = current_database()
            WHERE r.rolname = current_user
            """;

    private TestDatabaseSafety() {
    }

    /** Validates configuration without opening a connection or printing credentials. */
    public static void requireSafeTarget(String jdbcUrl, String username) {
        if (!USERNAME.equals(username)) {
            throw unsafe("use the dedicated eventorganizer_test login, not a developer/admin login");
        }
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw unsafe("an explicit PostgreSQL host and database are required");
        }
        final URI uri;
        try {
            uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            // URI exceptions may echo an entire URL containing credentials.
            throw unsafe("malformed JDBC URL");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                || uri.getRawAuthority().contains(",") || uri.getRawAuthority().endsWith(":")
                || uri.getPort() == 0 || uri.getPort() > 65535
                || !("/" + DATABASE).equals(uri.getRawPath())) {
            throw unsafe("only a single explicit host and event_organizer_test database are allowed");
        }
        if (uri.getRawQuery() != null) {
            Set<String> seen = new HashSet<>();
            for (String option : uri.getRawQuery().split("&", -1)) {
                String[] pair = option.split("=", 2);
                if (pair.length != 2) throw unsafe("malformed connection option");
                String key = decode(pair[0]);
                if (!seen.add(key)) throw unsafe("duplicate connection option");
                requireSafeOption(key, decode(pair[1]));
            }
        }
    }

    public static void requireSafeSchema(String schema) {
        if (schema == null || !schema.matches("[a-z][a-z0-9_]{0,62}")
                || schema.startsWith("pg_") || schema.equals("information_schema")) {
            throw unsafe("a single non-system schema identifier is required");
        }
    }

    /** Verifies the actual source and server identity before a caller may mutate data. */
    public static void requireSafeDataSource(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            if (hikari.getDataSource() != null || hikari.getDataSourceClassName() != null
                    || hikari.getDataSourceJNDI() != null || hikari.getConnectionInitSql() != null
                    || hikari.getConnectionTestQuery() != null) {
                throw unsafe("indirect Hikari configuration and pre-verification SQL hooks are not supported");
            }
            requireSafeTarget(hikari.getJdbcUrl(), hikari.getUsername());
            requireSafeProperties(hikari.getDataSourceProperties());
            if (hikari.getSchema() != null) requireSafeSchema(hikari.getSchema());
        } else if (dataSource instanceof AbstractDriverBasedDataSource driverSource) {
            requireSafeTarget(driverSource.getUrl(), driverSource.getUsername());
            requireSafeProperties(driverSource.getConnectionProperties());
        } else {
            throw unsafe("unsupported DataSource; validate its connection configuration explicitly");
        }

        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            try (var statement = connection.createStatement()) {
                statement.setQueryTimeout(5);
                try (var identity = statement.executeQuery(IDENTITY_QUERY)) {
                    if (!identity.next() || !DATABASE.equals(identity.getString("database_name"))
                            || !USERNAME.equals(identity.getString("role_name"))
                            || !USERNAME.equals(identity.getString("login_name"))
                            || identity.getBoolean("rolsuper") || identity.getBoolean("rolcreatedb")
                            || identity.getBoolean("rolcreaterole") || identity.getBoolean("rolreplication")
                            || identity.getBoolean("rolbypassrls") || identity.getBoolean("has_memberships")
                            || !identity.getBoolean("owns_database")) {
                        throw unsafe("the test login must own only the dedicated test database, without admin rights or role memberships");
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot verify the dedicated test database identity", exception);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static void requireSafeProperties(Properties properties) {
        if (properties == null) return;
        for (var entry : properties.entrySet()) {
            requireSafeOption(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    private static void requireSafeOption(String key, String value) {
        if (!CONNECTION_OPTIONS.contains(key) || value.isBlank()) {
            throw unsafe("unsupported or empty connection option; credential/session overrides are forbidden");
        }
        switch (key) {
            case "currentSchema" -> requireSafeSchema(value);
            case "sslmode" -> {
                if (!Set.of("disable", "allow", "prefer", "require", "verify-ca", "verify-full").contains(value)) {
                    throw unsafe("unsupported SSL mode");
                }
            }
            case "connectTimeout", "socketTimeout" -> {
                try {
                    if (!value.matches("[0-9]+") || Integer.parseInt(value) <= 0) {
                        throw unsafe("connection timeouts must be positive seconds");
                    }
                } catch (NumberFormatException exception) {
                    throw unsafe("invalid connection timeout");
                }
            }
            default -> { /* ApplicationName does not change the target or session privileges. */ }
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw unsafe("malformed connection option encoding");
        }
    }

    private static IllegalStateException unsafe(String reason) {
        return new IllegalStateException("Unsafe test database: " + reason);
    }
}
