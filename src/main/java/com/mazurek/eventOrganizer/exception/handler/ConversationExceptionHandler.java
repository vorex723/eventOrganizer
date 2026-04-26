package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.conversation.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ConversationExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(MessagingYourselfException.class)
    public ResponseEntity<ErrorMessageDto> handleMessagingYourselfException(MessagingYourselfException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleConversationNotFoundException(ConversationNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }
}
