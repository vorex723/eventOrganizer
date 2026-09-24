ALTER TABLE events DROP CONSTRAINT chk_events_max_attendees;
ALTER TABLE events ALTER COLUMN max_attendees DROP NOT NULL;
ALTER TABLE events ADD CONSTRAINT chk_events_max_attendees
    CHECK (max_attendees IS NULL OR max_attendees >= 1);
