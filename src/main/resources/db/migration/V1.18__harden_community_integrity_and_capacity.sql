-- Canonicalize cities before enforcing case- and whitespace-insensitive uniqueness.
WITH duplicate_cities AS (
    SELECT c.id AS duplicate_id,
           (SELECT canonical.id
            FROM cities canonical
            WHERE lower(btrim(canonical.name)) = lower(btrim(c.name))
            ORDER BY canonical.id
            LIMIT 1) AS canonical_id
    FROM cities c
), city_duplicates AS (
    SELECT duplicate_id, canonical_id FROM duplicate_cities WHERE duplicate_id <> canonical_id
)
UPDATE events e SET city_id = d.canonical_id FROM city_duplicates d WHERE e.city_id = d.duplicate_id;

WITH duplicate_cities AS (
    SELECT c.id AS duplicate_id,
           (SELECT canonical.id
            FROM cities canonical
            WHERE lower(btrim(canonical.name)) = lower(btrim(c.name))
            ORDER BY canonical.id
            LIMIT 1) AS canonical_id
    FROM cities c
), city_duplicates AS (
    SELECT duplicate_id, canonical_id FROM duplicate_cities WHERE duplicate_id <> canonical_id
)
UPDATE users u SET city_id = d.canonical_id FROM city_duplicates d WHERE u.city_id = d.duplicate_id;

WITH duplicate_cities AS (
    SELECT c.id AS duplicate_id,
           (SELECT canonical.id
            FROM cities canonical
            WHERE lower(btrim(canonical.name)) = lower(btrim(c.name))
            ORDER BY canonical.id
            LIMIT 1) AS canonical_id
    FROM cities c
)
DELETE FROM cities c USING duplicate_cities d WHERE c.id = d.duplicate_id AND d.duplicate_id <> d.canonical_id;

UPDATE cities SET name = lower(btrim(name));
CREATE UNIQUE INDEX uq_cities_normalized_name ON cities ((lower(btrim(name))));

-- Retain one tag for each normalized name and preserve every event association.
WITH duplicate_tags AS (
    SELECT t.id AS duplicate_id,
           (SELECT canonical.id
            FROM tags canonical
            WHERE lower(btrim(canonical.name)) = lower(btrim(t.name))
            ORDER BY canonical.id
            LIMIT 1) AS canonical_id
    FROM tags t
), tag_duplicates AS (
    SELECT duplicate_id, canonical_id FROM duplicate_tags WHERE duplicate_id <> canonical_id
)
INSERT INTO event_tag (event_id, tag_id)
SELECT DISTINCT et.event_id, d.canonical_id
FROM event_tag et
JOIN tag_duplicates d ON et.tag_id = d.duplicate_id
ON CONFLICT DO NOTHING;

WITH duplicate_tags AS (
    SELECT t.id AS duplicate_id,
           (SELECT canonical.id
            FROM tags canonical
            WHERE lower(btrim(canonical.name)) = lower(btrim(t.name))
            ORDER BY canonical.id
            LIMIT 1) AS canonical_id
    FROM tags t
), tag_duplicates AS (
    SELECT duplicate_id, canonical_id FROM duplicate_tags WHERE duplicate_id <> canonical_id
)
DELETE FROM event_tag et USING tag_duplicates d WHERE et.tag_id = d.duplicate_id;

WITH duplicate_tags AS (
    SELECT t.id AS duplicate_id,
           (SELECT canonical.id
            FROM tags canonical
            WHERE lower(btrim(canonical.name)) = lower(btrim(t.name))
            ORDER BY canonical.id
            LIMIT 1) AS canonical_id
    FROM tags t
)
DELETE FROM tags t USING duplicate_tags d WHERE t.id = d.duplicate_id AND d.duplicate_id <> d.canonical_id;

UPDATE tags SET name = lower(btrim(name));
CREATE UNIQUE INDEX uq_tags_normalized_name ON tags ((lower(btrim(name))));

ALTER TABLE events ADD COLUMN max_attendees integer NOT NULL DEFAULT 1000;
ALTER TABLE events ADD CONSTRAINT chk_events_max_attendees CHECK (max_attendees BETWEEN 1 AND 1000);
ALTER TABLE events ADD COLUMN attendee_count integer NOT NULL DEFAULT 0;
UPDATE events e
SET attendee_count = (SELECT COUNT(*) FROM event_user eu WHERE eu.event_id = e.id);

CREATE INDEX idx_events_owner_start_id ON events (user_id, event_start_date DESC, id DESC);
CREATE INDEX idx_events_city_start_id ON events (city_id, event_start_date DESC, id DESC);
CREATE INDEX idx_event_user_user_event ON event_user (user_id, event_id);
CREATE INDEX idx_files_event_upload_id ON files (event_id, upload_date_time ASC, id ASC);
CREATE INDEX idx_threads_event_last_activity_id ON threads (event_id, last_activity DESC, id DESC);
CREATE INDEX idx_thread_replies_thread_date_id ON thread_replies (thread_id, reply_date ASC, id ASC);
