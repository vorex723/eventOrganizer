package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.dto.*;
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

    @PostMapping("/conversations/direct")
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

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageDto> sendMessageToConversation(
            @PathVariable(name = "conversationId") UUID conversationId,
            @Valid @RequestBody SendConversationMessageDto sendConversationMessageDto)
    {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.sendMessageToConversation(conversationId, sendConversationMessageDto));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ConversationOverviewPageDto> getConversations(
            @RequestParam(name = "page", required = false, defaultValue = "0") int pageNumber)
    {
        return ResponseEntity.ok().body(conversationService.getConversations(pageNumber));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDetailsDto> getConversation(@PathVariable UUID conversationId) {
        return ResponseEntity.ok().body(conversationService.getConversation(conversationId));
    }


}
