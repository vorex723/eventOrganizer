-- Breaking change before the first deployment: recreate populated development databases.
-- No name-based backfill or calls to an external provider are performed.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cities) THEN
        RAISE EXCEPTION 'City refactor requires an empty database. Recreate the development database; legacy city data is intentionally unsupported.';
    END IF;
END $$;

ALTER TABLE cities DROP CONSTRAINT ukl61tawv0e2a93es77jkyvi7qa;
DROP INDEX uq_cities_normalized_name;

ALTER TABLE cities
    ADD COLUMN external_id varchar(255) NOT NULL,
    ADD COLUMN country_code varchar(2) NOT NULL,
    ADD COLUMN admin_area varchar(255),
    ADD COLUMN latitude double precision NOT NULL,
    ADD COLUMN longitude double precision NOT NULL,
    ALTER COLUMN name SET NOT NULL,
    ADD CONSTRAINT uk_city_external_id UNIQUE (external_id),
    ADD CONSTRAINT chk_city_external_id CHECK (length(btrim(external_id)) > 0),
    ADD CONSTRAINT chk_city_name CHECK (length(btrim(name)) > 0),
    ADD CONSTRAINT chk_city_country_code CHECK (country_code ~ '^[A-Z]{2}$'),
    ADD CONSTRAINT chk_city_latitude CHECK (latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT chk_city_longitude CHECK (longitude BETWEEN -180 AND 180);
