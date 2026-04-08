package com.mazurek.eventOrganizer.thread;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ThreadReplyRepository extends JpaRepository<ThreadReply, UUID> {
    Optional<ThreadReply> findByIdAndThreadId(UUID threadReplyId, UUID threadId);
    Set<ThreadReply> findByThreadId(UUID threadId);
    Set<ThreadReply> findByReplierId(UUID replierId);
}
