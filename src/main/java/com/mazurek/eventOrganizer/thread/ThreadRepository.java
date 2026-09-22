package com.mazurek.eventOrganizer.thread;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ThreadRepository extends JpaRepository<Thread, UUID> {
    Optional<Thread> findByIdAndEventId(UUID threadId, UUID eventId);
    Set<Thread> findByEventId(UUID eventId);
    Set<Thread> findByOwnerId(UUID ownerId);

    @Query("SELECT t FROM Thread t WHERE t.owner.id = :userId AND t.event IN " +
            "(SELECT e FROM Event e JOIN e.attendingUsers u WHERE u.id = :userId)")
    List<Thread> findActiveThreadsByUserId(@Param("userId") UUID userId);

    boolean existsByIdAndEventId(UUID threadId, UUID eventId);

    Page<Thread> findByEventId(UUID eventId, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Thread thread
            SET thread.replyCount = thread.replyCount + 1,
                thread.lastActivity = CASE
                    WHEN thread.lastActivity IS NULL OR thread.lastActivity < :activity THEN :activity
                    ELSE thread.lastActivity
                END
            WHERE thread.id = :threadId
            """)
    int incrementReplyCountAndAdvanceLastActivity(
            @Param("threadId") UUID threadId,
            @Param("activity") java.time.Instant activity
    );
}
