package com.mazurek.eventOrganizer.conversation.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);

}
