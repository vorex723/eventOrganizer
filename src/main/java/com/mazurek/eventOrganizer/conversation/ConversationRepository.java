package com.mazurek.eventOrganizer.conversation;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Conversation c
            JOIN c.participants participant
            WHERE c.id = :conversationId
              AND participant.user.id = :userId
              AND participant.leftAt IS NULL
            """)
    boolean existsByIdAndParticipant(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Query("""
            SELECT c FROM Conversation c
            JOIN c.participants participant
            WHERE participant.user.id = :participantId
            AND participant.leftAt IS NULL
            """)
    Page<Conversation> findByParticipantId(@Param("participantId") UUID participantId, Pageable pageable);

    @Query("""
            SELECT c FROM Conversation c
                 JOIN c.participants participant
                 WHERE participant.user.id = :participantId
                 AND participant.leftAt IS NULL
                 AND c.id = :conversationId
            """)
    Optional<Conversation> findByIdAndParticipantId(UUID conversationId, UUID participantId);
}
