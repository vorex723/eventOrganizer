-- Disposable PostgreSQL test instance only. Run as its bootstrap administrator.
-- Do not run this against an existing development or production database.
CREATE ROLE eventorganizer_test WITH LOGIN PASSWORD 'test-only-password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
ALTER DATABASE event_organizer_test OWNER TO eventorganizer_test;
