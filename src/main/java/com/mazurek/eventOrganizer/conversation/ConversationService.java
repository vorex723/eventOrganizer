package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.dto.ConversationOverviewPageDto;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.conversation.dto.MessagePageDto;
import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;

import java.util.UUID;

public interface ConversationService {

    ConversationOverviewPageDto getConversations(int pageNumber);
    DirectMessageResponseDto sendDirectMessage(SendDirectMessageDto sendDirectMessageDto);
    MessagePageDto getMessagesInConversation(UUID conversationId, int pageNumber);

}
