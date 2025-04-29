package com.mazurek.eventOrganizer.exception;

import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.exception.converastion.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.converastion.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.exception.search.NoSearchResultException;
import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessageDto> handleRuntimeException(RuntimeException exception){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleEventNotFoundException(EventNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(ThreadNotFoundInEventException.class)
    public ResponseEntity<ErrorMessageDto> handleThreadNotFoundInEventException(ThreadNotFoundInEventException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ReplyNotFoundInThreadException.class)
    public ResponseEntity<ErrorMessageDto> handleThreadNotFoundInEventException(ReplyNotFoundInThreadException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }


    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidEventStartDateException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidEventStartDateException(InvalidEventStartDateException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NotEventOwnerException.class)
    public ResponseEntity<ErrorMessageDto> handleNotEventOwnerException(NotEventOwnerException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EventAlreadyHadPlaceException.class)
    public ResponseEntity<ErrorMessageDto> handleEventAlreadyHadPlaceException(EventAlreadyHadPlaceException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EventOwnerAlreadyAttendsEventException.class)
    public ResponseEntity<ErrorMessageDto> handleEventOwnerAlreadyAttendsEventException(EventOwnerAlreadyAttendsEventException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ThreadNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleThreadNotFoundException(ThreadNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NotEventAttenderException.class)
    public ResponseEntity<ErrorMessageDto> handleNotAttenderException(NotEventAttenderException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NotThreadOwnerException.class)
    public ResponseEntity<ErrorMessageDto> handleNotThreadOwnerException(NotThreadOwnerException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(WrongThreadException.class)
    public ResponseEntity<ErrorMessageDto> handleWrongThreadException(WrongThreadException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({NotThreadReplyOwnerException.class})
    public ResponseEntity<ErrorMessageDto> handleNotThreadOwnerException(NotThreadReplyOwnerException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NoSearchResultException.class)
    public ResponseEntity<ErrorMessageDto> handleNoSearchResultException(NoSearchResultException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorMessageDto> handleIOException(IOException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(FileTypeNotAllowedException.class)
    public ResponseEntity<ErrorMessageDto> handleFileTypeNotAllowedException(FileTypeNotAllowedException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleFileNotFoundException(FileNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MessagingYourselfException.class)
    public ResponseEntity<ErrorMessageDto> handleMessagingYourselfException(MessagingYourselfException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleConversationNotFoundException(ConversationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));

    }
    @org.springframework.web.bind.annotation.ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleCityNotFoundException(CityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleTagNotFoundException(TagNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidUserException(InvalidUserException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidPasswordException(InvalidPasswordException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(NotMatchingPasswordsException.class)
    public ResponseEntity<ErrorMessageDto> handleNotMatchingPasswordException(NotMatchingPasswordsException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ErrorMessageDto> handleUserAlreadyExistException(UserAlreadyExistException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidEmailException(InvalidEmailException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleNotificationNotFoundException(NotificationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageDto> handleIllegalArgumentException(IllegalArgumentException exception) {
        exception.printStackTrace();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

}
