package com.mazurek.eventOrganizer.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

   // Page<Event> findByOwnerId(UUID id, Pageable pageable);
    @Query(
            value = "SELECT e.* FROM events e, event_user eu WHERE eu.user_id = :id AND e.id = eu.event_id",
            nativeQuery = true)
    Page<Event> findUserAttendingEventsByUserId(@Param("id") UUID id, Pageable pageable);
   /* @Query(
           value = "SELECT * FROM events e WHERE e.user_id = :id AND e.event_start_date > NOW()",
           countQuery = "SELECT count(*) FROM events e WHERE e.user_id = :id AND e.event_start_date > NOW()",
           nativeQuery = true)*/
    @Query(value = "SELECT * FROM events e WHERE e.user_id = :id AND e.event_start_date > NOW()", nativeQuery = true)
    Page<Event> findEventsByOwnerId(@Param("id") UUID id, Pageable pageable);
   // Page<Event> customQueryUserAttendingEvents(@Param("id") UUID id, Pageable pageable);
    Set<Event> findByTagsName(String tagName);
    Set<Event> findByIgnoreCaseTagsNameIn(List<String> tagName);
    HashSet<Event> findByIgnoreCaseNameContaining(String searchString);

}
