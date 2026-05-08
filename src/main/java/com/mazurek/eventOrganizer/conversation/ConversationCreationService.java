package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;
import java.util.UUID;

public interface ConversationCreationService {
    UUID createDirectConversation(User sender, User recipient, Instant createdAt);
}
