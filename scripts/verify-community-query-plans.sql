-- Run with psql against a representative PostgreSQL dataset after V1.18.
-- Replace the psql variables with real IDs from that dataset, for example:
-- psql "$APP_DB_URL" -v owner_id='...' -v attendee_id='...' -v city_id='...'
--      -v event_id='...' -v thread_id='...' -f scripts/verify-community-query-plans.sql

EXPLAIN (ANALYZE, BUFFERS)
SELECT e.id, e.event_start_date
FROM events e
WHERE e.user_id = :'owner_id'
ORDER BY e.event_start_date DESC, e.id DESC
LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT e.id, e.event_start_date
FROM events e
WHERE e.user_id = :'owner_id'
  AND e.event_start_date > now()
ORDER BY e.event_start_date DESC, e.id DESC
LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT e.id, e.event_start_date
FROM event_user eu
JOIN events e ON e.id = eu.event_id
WHERE eu.user_id = :'attendee_id'
ORDER BY e.event_start_date DESC, e.id DESC
LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT e.id, e.event_start_date
FROM events e
WHERE e.city_id = :'city_id'
ORDER BY e.event_start_date DESC, e.id DESC
LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT f.id, f.upload_date_time
FROM files f
WHERE f.event_id = :'event_id'
ORDER BY f.upload_date_time ASC, f.id ASC
LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT t.id, t.last_activity
FROM threads t
WHERE t.event_id = :'event_id'
ORDER BY t.last_activity DESC, t.id DESC
LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT r.id, r.reply_date
FROM thread_replies r
WHERE r.thread_id = :'thread_id'
ORDER BY r.reply_date ASC, r.id ASC
LIMIT 20;
