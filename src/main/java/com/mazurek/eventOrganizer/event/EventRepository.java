package com.mazurek.eventOrganizer.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByOwnerId(UUID id, Pageable pageable);

    @Query("SELECT e FROM Event e JOIN e.attendingUsers u WHERE u.id = :id")
    Page<Event> findUserAttendingEventsByUserId(@Param("id") UUID id, Pageable pageable);

    @Query("SELECT e FROM Event e JOIN e.attendingUsers u WHERE u.id = :id AND e.eventStartDate > :now")
    Page<Event> findUpcomingUserAttendingEventsByUserId(@Param("id") UUID id, @Param("now") Instant now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.owner.id = :id AND e.eventStartDate > :now")
    Page<Event> findUpcomingEventsByOwnerId(@Param("id") UUID id, @Param("now") Instant now, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM Event e JOIN e.attendingUsers u
            WHERE e.id = :eventId AND u.id = :userId
            """)
    boolean isUserAttendingEvent(@Param("userId") UUID userId, @Param("eventId") UUID eventId);
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e WHERE e.id = :eventId AND e.owner.id = :userId")
    boolean isUserEventOwner(@Param("userId") UUID userId, @Param("eventId") UUID eventId);

    @Query("""
    SELECT CASE WHEN COUNT(DISTINCT e) > 0 THEN true ELSE false END
    FROM Event e
    LEFT JOIN e.attendingUsers u
    WHERE e.id = :eventId
      AND (e.owner.id = :userId OR u.id = :userId)
    """)
    boolean isUserAttenderOrOwner(@Param("userId") UUID userId, @Param("eventId") UUID eventId);
}
