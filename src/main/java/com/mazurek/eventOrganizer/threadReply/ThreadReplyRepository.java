package com.mazurek.eventOrganizer.threadReply;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ThreadReplyRepository extends JpaRepository<ThreadReply, UUID> {

    Optional<ThreadReply> findByIdAndThreadId(UUID threadReplyId, UUID threadId);
    Page<ThreadReply> findByThreadId(UUID threadId, Pageable pageable);
}
