# Dedicated test database

Database-backed tests use only `event_organizer_test`, with the login
`eventorganizer_test`. Hosts and ports may vary; database and login names are
intentional safety policy, not unrestricted configuration.

```properties
APP_TEST_DB_URL=jdbc:postgresql://localhost:5432/event_organizer_test
APP_TEST_DB_USERNAME=eventorganizer_test
APP_TEST_DB_PASSWORD=test-only-password
```

These defaults are for disposable local test infrastructure only. The password
can be overridden. Maven does not automatically load `.env`; provide environment
variables to the shell or IntelliJ runner. Never substitute `APP_DB_*` credentials.

## Provisioning

Use a separate, disposable PostgreSQL instance or a deliberately provisioned test
database. As its administrator, create the **new empty** `event_organizer_test`
database, then run `init-test-role.sql` against that instance. The script creates
a login without superuser, database/role creation, replication or RLS-bypass
privileges, and makes it the owner of the test database. It is not an application
Flyway migration and must not be put in `db/migration`.

For a newly created dedicated instance, for example:

```bash
# Set the host/port to your dedicated test instance, not the application database.
createdb -h 127.0.0.1 -p 55432 -U postgres event_organizer_test
psql -h 127.0.0.1 -p 55432 -U postgres -d event_organizer_test \
  -v ON_ERROR_STOP=1 -f src/test/resources/database/init-test-role.sql
```

Do not run these provisioning commands blindly against an existing database.
The script intentionally fails if the role already exists; inspect existing
ownership and privileges rather than resetting data or silently upgrading it.
The local test Compose definition initializes the role on a fresh instance.
`POSTGRES_USER=eventorganizer_test` alone is **not** equivalent: the PostgreSQL
image makes its bootstrap user a superuser, which the guard rejects.

## What is protected

The factory registered in test-only `META-INF/spring.factories` installs a guard
in Spring TestContext runs. Effective connection properties are checked before
database beans are instantiated. Actual supported DataSources and Flyway sources
are checked before they are exposed to consumers/migration initialization.
The server identity check performs SELECT only: database, login/current role,
database ownership, admin flags and role memberships.

`DeletionService` checks again before global cleanup. Standalone baseline Flyway
and owned-schema cleanup paths also invoke the guard explicitly. Transactional
cleanup reuses the transaction's connection; the check does not close it or
consume another pool slot. Focused contexts without a database need no DB setup.

Allowed sources are JDBC-configured Hikari and Spring driver-based DataSources.
Indirect/JNDI/custom sources and Hikari SQL initialization/test hooks are refused.
The JDBC URL must specify one host and the exact test database, without embedded
credentials, multi-host syntax or session/credential overrides. Supported query
options are `currentSchema`, `sslmode`, positive `connectTimeout`/`socketTimeout`
and `ApplicationName`. Schemas must be single lowercase non-system identifiers.

This is protection against accidental misconfiguration, not a security sandbox
or a complete ACL audit of other databases. Provision least privilege on the
dedicated instance and never reuse this login for application/production data.
Schema-name validation does not prove ownership: tests must still drop only the
schema they created. Arbitrary side effects inside custom bean factories, direct
JDBC calls, `ApplicationContextRunner` or shell scripts are not automatically
intercepted; new manual database entrypoints must validate before mutation.

## Execution

```bash
# No database/network needed: parser, source identity mocks and focused guard contexts.
./mvnw -B -Dtest=TestDatabaseSafetyUnitTest,TestDatabaseSafetyContextIntegrationTest test

# Requires the dedicated test DB; external providers remain excluded.
./mvnw -B clean verify
```

Ordinary full-context integrations declare `@ActiveProfiles("test")`; IntelliJ
JUnit uses that annotation just as Maven does. Do not globally force `test` on
configuration tests: their explicit `local`, `production` or mixed profiles are
the subject of those tests. Focused contexts and pure Mockito tests do not require
a profile merely because they live under `src/test`.

In IntelliJ, exclude the `external` tag (`!external`) and supply the dedicated
`APP_TEST_DB_*` credentials for DB-backed runs. Existing configurations using
`postgres` or `devuser` fail intentionally; provision the dedicated login and
update `APP_TEST_DB_*`. Explicit profiles do not provision the database or bypass
the safety guard. Maven/resource profile defaults remain as fallback; the IDE
does not inherit Maven's external-tag exclusion.
