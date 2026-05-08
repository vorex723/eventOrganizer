package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.message.Message;
import org.springframework.data.domain.Page;

import java.util.List;

public record MessagePageDto(
        List<MessageDto> messages,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {
    public MessagePageDto(Page<Message> messagePage){
        this(
                messagePage.getContent().stream().map(MessageDto::new).toList(),
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.isLast()
        );
    }
}
