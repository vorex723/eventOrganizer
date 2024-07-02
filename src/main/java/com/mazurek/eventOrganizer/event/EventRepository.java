package com.mazurek.eventOrganizer.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Set<Event> findByTagsName(String tagName);
    Set<Event> findByIgnoreCaseTagsNameIn(List<String> tagName);
    HashSet<Event> findByIgnoreCaseNameContaining(String searchString);
}
