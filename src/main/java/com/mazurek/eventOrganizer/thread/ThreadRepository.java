package com.mazurek.eventOrganizer.thread;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreadRepository extends JpaRepository<Thread, UUID> {
    Optional<Thread> findByIdAndEventId(UUID threadId, UUID eventId);
    List<Thread> findByEventId(UUID eventId);

}
