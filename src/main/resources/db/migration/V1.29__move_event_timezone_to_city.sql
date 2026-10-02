-- Geographic timezones must come from Geoapify, not legacy event/user preferences.
-- Before deployment, recreate populated development databases instead of guessing a backfill.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cities) THEN
        RAISE EXCEPTION 'City timezone refactor requires an empty database. Recreate the development database; timezone backfill is intentionally unsupported.';
    END IF;
END $$;

ALTER TABLE cities
    ADD COLUMN time_zone_id varchar(255) NOT NULL,
    ADD CONSTRAINT chk_city_time_zone_id CHECK (length(btrim(time_zone_id)) > 0);

ALTER TABLE events DROP COLUMN time_zone_id;

-- Event timezone is derived from its city, so the relation is mandatory.
ALTER TABLE events ALTER COLUMN city_id SET NOT NULL;
