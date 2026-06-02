package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.dto.ConversationOverviewPageDto;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.conversation.dto.MessagePageDto;
import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ConversationController {
    private final ConversationService conversationService;

    @PostMapping("/messages/direct")
    public ResponseEntity<DirectMessageResponseDto> sendDirectMessage(@Valid @RequestBody SendDirectMessageDto sendDirectMessageDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.sendDirectMessage(sendDirectMessageDto));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessagePageDto> getMessagesInConversation(
            @PathVariable(name = "conversationId") UUID conversationId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int pageNumber)
    {
        return ResponseEntity.ok().body(conversationService.getMessagesInConversation(conversationId, pageNumber));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ConversationOverviewPageDto> getConversations(
            @RequestParam(name = "page", required = false, defaultValue = "0") int pageNumber)
    {
        return ResponseEntity.ok().body(conversationService.getConversations(pageNumber));
    }


}
