package com.mazurek.eventOrganizer.thread;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadJpaRepository extends JpaRepository<Thread, Long> {
}
