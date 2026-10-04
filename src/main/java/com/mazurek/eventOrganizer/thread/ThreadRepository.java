package com.mazurek.eventOrganizer.thread;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ThreadRepository extends JpaRepository<Thread, UUID> {
    Optional<Thread> findByIdAndEventId(UUID threadId, UUID eventId);

    boolean existsByIdAndEventId(UUID threadId, UUID eventId);

    Page<Thread> findByEventId(UUID eventId, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Thread thread
            SET thread.replyCount = thread.replyCount + 1,
                thread.lastActivity = CASE
                    WHEN thread.lastActivity IS NULL OR thread.lastActivity < :activity THEN :activity
                    ELSE thread.lastActivity
                END,
                thread.version = thread.version + 1
            WHERE thread.id = :threadId
            """)
    int incrementReplyCountAndAdvanceLastActivity(
            @Param("threadId") UUID threadId,
            @Param("activity") java.time.Instant activity
    );
}
