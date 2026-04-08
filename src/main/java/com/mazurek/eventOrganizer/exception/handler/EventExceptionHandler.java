package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.event.AlreadyAttendingEventException;
import com.mazurek.eventOrganizer.exception.event.EventAlreadyHadPlaceException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.EventOwnerAlreadyAttendsEventException;
import com.mazurek.eventOrganizer.exception.event.EventOwnerMustAttendEventException;
import com.mazurek.eventOrganizer.exception.event.InvalidEventStartDateException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.event.NotEventOwnerException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class EventExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleEventNotFoundException(EventNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(InvalidEventStartDateException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidEventStartDateException(InvalidEventStartDateException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(NotEventOwnerException.class)
    public ResponseEntity<ErrorMessageDto> handleNotEventOwnerException(NotEventOwnerException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(EventAlreadyHadPlaceException.class)
    public ResponseEntity<ErrorMessageDto> handleEventAlreadyHadPlaceException(EventAlreadyHadPlaceException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(EventOwnerAlreadyAttendsEventException.class)
    public ResponseEntity<ErrorMessageDto> handleEventOwnerAlreadyAttendsEventException(EventOwnerAlreadyAttendsEventException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(AlreadyAttendingEventException.class)
    public ResponseEntity<ErrorMessageDto> handleAlreadyAttendingEventException(AlreadyAttendingEventException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(EventOwnerMustAttendEventException.class)
    public ResponseEntity<ErrorMessageDto> handleEventOwnerMustAttendEventException(EventOwnerMustAttendEventException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(NotEventAttenderException.class)
    public ResponseEntity<ErrorMessageDto> handleNotAttenderException(NotEventAttenderException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }
}
