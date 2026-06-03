package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.dto.*;

import java.util.UUID;

public interface ConversationService {

    ConversationDetailsDto getConversation(UUID conversationId);
    ConversationOverviewPageDto getConversations(int pageNumber);
    DirectMessageResponseDto sendDirectMessage(SendDirectMessageDto sendDirectMessageDto);
    MessageDto sendMessageToConversation(UUID conversationId, SendConversationMessageDto sendConversationMessageDto);
    MessagePageDto getMessagesInConversation(UUID conversationId, int pageNumber);


}
