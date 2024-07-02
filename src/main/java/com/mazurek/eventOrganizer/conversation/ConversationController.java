package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.dto.SendMessageDto;
import com.mazurek.eventOrganizer.exception.converastion.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.converastion.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ConversationController {
    private final ConversationService conversationService;

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageDto sendMessageDto, @RequestHeader("Authorization") String jwtToken ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.sendMessage(sendMessageDto, jwtToken.substring(7)));
        } catch (UserNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        } catch (MessagingYourselfException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversationById(@PathVariable(name = "conversationId") UUID conversationId, @RequestHeader("Authorization") String jwtToken){
        try{
        return ResponseEntity.status(HttpStatus.OK).body(conversationService.getConversationById(conversationId, jwtToken.substring(7)));
        } catch (ConversationNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        } catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
