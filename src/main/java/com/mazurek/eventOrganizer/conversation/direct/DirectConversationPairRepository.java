package com.mazurek.eventOrganizer.conversation.direct;


import com.mazurek.eventOrganizer.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DirectConversationPairRepository extends JpaRepository<DirectConversationPair, Long> {
    @Query("""
        SELECT pair.conversation
        FROM DirectConversationPair pair
        WHERE pair.firstUserId = :firstUserId
        AND pair.secondUserId = :secondUserId
    """)
    Optional<Conversation> findConversationByUsers(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId);
}
