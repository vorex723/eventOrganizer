ALTER TABLE threads
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE thread_replies
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE threads thread
SET reply_count = (
        SELECT COUNT(*)
        FROM thread_replies reply
        WHERE reply.thread_id = thread.id
    ),
    last_activity = GREATEST(
        COALESCE(thread.last_activity, thread.create_date),
        COALESCE(
            (
                SELECT MAX(reply.reply_date)
                FROM thread_replies reply
                WHERE reply.thread_id = thread.id
            ),
            thread.create_date
        )
    );
