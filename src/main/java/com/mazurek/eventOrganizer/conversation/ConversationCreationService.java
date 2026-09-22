package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.conversation.dto.MessageDto;

import java.time.Instant;
import java.util.UUID;

public interface ConversationCreationService {
    UUID createDirectConversation(User sender, User recipient, Instant createdAt);

    InitialDirectMessage createDirectConversationWithInitialMessage(
            User sender,
            User recipient,
            String content,
            Instant createdAt
    );

    record InitialDirectMessage(UUID conversationId, MessageDto message) {
    }
}
