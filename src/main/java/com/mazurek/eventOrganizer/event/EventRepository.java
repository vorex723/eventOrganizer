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

}
