package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.Conversation;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public record ConversationOverviewPageDto(
        List<ConversationOverviewDto> conversations,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {
    public ConversationOverviewPageDto(Page<Conversation> conversationPage, UUID userId) {
        this(
                conversationPage.getContent().stream().map(conversation -> new ConversationOverviewDto(conversation, userId)).toList(),
                conversationPage.getNumber(),
                conversationPage.getSize(),
                conversationPage.getTotalElements(),
                conversationPage.getTotalPages(),
                conversationPage.isLast()
        );
    }
}
