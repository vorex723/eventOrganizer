package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findById(Long id);

    /*@Query("SELECT DISTINCT e.* FROM Events e JOIN event_tag et ON e.id = et.event_id JOIN tags t ON et.tag_id = t.id WHERE t.name = :tagName;")
    Set<Event> findByTagName(@Param("tagName") String tagName);*/
    Set<Event> findByTagsName(String tagName);
    Set<Event> findByTagsNameIn(List<String> tagName);
    HashSet<Event> findByNameContaining(String searchString);
}
