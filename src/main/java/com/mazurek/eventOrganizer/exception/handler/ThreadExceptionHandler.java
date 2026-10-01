package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadReplyOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ReplyNotFoundInThreadException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
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
        return buildErrorResponse(HttpStatus.NOT_FOUND, ApiErrorCode.THREAD_NOT_FOUND_IN_EVENT, exception);
    }

    @ExceptionHandler(ReplyNotFoundInThreadException.class)
    public ResponseEntity<ErrorMessageDto> handleReplyNotFoundInThreadException(ReplyNotFoundInThreadException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ApiErrorCode.REPLY_NOT_FOUND_IN_THREAD, exception);
    }

    @ExceptionHandler(NotThreadOwnerException.class)
    public ResponseEntity<ErrorMessageDto> handleNotThreadOwnerException(NotThreadOwnerException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ApiErrorCode.NOT_THREAD_OWNER, exception);
    }

    @ExceptionHandler(NotThreadReplyOwnerException.class)
    public ResponseEntity<ErrorMessageDto> handleNotThreadReplyOwnerException(NotThreadReplyOwnerException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ApiErrorCode.NOT_THREAD_REPLY_OWNER, exception);
    }
}
