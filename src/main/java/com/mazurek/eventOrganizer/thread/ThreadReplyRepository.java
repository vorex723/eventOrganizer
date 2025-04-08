package com.mazurek.eventOrganizer.thread;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ThreadReplyRepository extends JpaRepository<ThreadReply, UUID> {
}
