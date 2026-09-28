package com.mazurek.eventOrganizer.conversation.participant;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID sender);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ConversationParticipant participant
            SET participant.leftAt = :leftAt
            WHERE participant.user.id = :userId
              AND participant.leftAt IS NULL
              AND participant.conversation.type = com.mazurek.eventOrganizer.conversation.ConversationType.GROUP
            """)
    int markGroupParticipantsLeftByUserId(
            @Param("userId") UUID userId,
            @Param("leftAt") Instant leftAt
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ConversationParticipant participant
            SET participant.lastReadMessageId = :lastReadMessageId,
                participant.lastReadAt = CASE
                    WHEN participant.lastReadAt IS NULL OR participant.lastReadAt < :readAt THEN :readAt
                    ELSE participant.lastReadAt
                END
            WHERE participant.conversation.id = :conversationId
              AND participant.user.id = :userId
              AND participant.leftAt IS NULL
              AND (participant.lastReadMessageId IS NULL OR participant.lastReadMessageId < :lastReadMessageId)
            """)
    int advanceLastReadMessage(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("readAt") Instant readAt
    );

}
