package com.mazurek.eventOrganizer.threadReply;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ThreadReplyRepository extends JpaRepository<ThreadReply, UUID> {

    @Modifying(flushAutomatically = true)
    @Query("""
            update ThreadReply reply
            set reply.replierNameAtCreation = 'Deleted user'
            where reply.replier.id = :userId
            """)
    int anonymizeReplierSnapshotsByUserId(@Param("userId") UUID userId);
    Optional<ThreadReply> findByIdAndThreadId(UUID threadReplyId, UUID threadId);
    Set<ThreadReply> findByThreadId(UUID threadId);
    Set<ThreadReply> findByReplierId(UUID replierId);
    Page<ThreadReply> findByThreadId(UUID threadId, Pageable pageable);
}
