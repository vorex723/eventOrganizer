package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadReplyOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ReplyNotFoundInThreadException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.thread.ThreadReplyNotFoundException;
import com.mazurek.eventOrganizer.exception.thread.WrongThreadException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ThreadExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(ThreadNotFoundInEventException.class)
    public ResponseEntity<ErrorMessageDto> handleThreadNotFoundInEventException(ThreadNotFoundInEventException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(ReplyNotFoundInThreadException.class)
    public ResponseEntity<ErrorMessageDto> handleReplyNotFoundInThreadException(ReplyNotFoundInThreadException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(ThreadNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleThreadNotFoundException(ThreadNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(ThreadReplyNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleThreadReplyNotFoundException(ThreadReplyNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(NotThreadOwnerException.class)
    public ResponseEntity<ErrorMessageDto> handleNotThreadOwnerException(NotThreadOwnerException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(WrongThreadException.class)
    public ResponseEntity<ErrorMessageDto> handleWrongThreadException(WrongThreadException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(NotThreadReplyOwnerException.class)
    public ResponseEntity<ErrorMessageDto> handleNotThreadReplyOwnerException(NotThreadReplyOwnerException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }
}
