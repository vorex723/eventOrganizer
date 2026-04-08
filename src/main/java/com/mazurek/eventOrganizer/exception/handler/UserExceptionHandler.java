package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.user.InvalidEmailException;
import com.mazurek.eventOrganizer.exception.user.InvalidPasswordException;
import com.mazurek.eventOrganizer.exception.user.InvalidUserException;
import com.mazurek.eventOrganizer.exception.user.NotMatchingEmailsException;
import com.mazurek.eventOrganizer.exception.user.NotMatchingPasswordsException;
import com.mazurek.eventOrganizer.exception.user.SameEmailException;
import com.mazurek.eventOrganizer.exception.user.UserAccountNotActivatedException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.exception.user.UserBannedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserRoleNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class UserExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidUserException(InvalidUserException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidPasswordException(InvalidPasswordException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(NotMatchingPasswordsException.class)
    public ResponseEntity<ErrorMessageDto> handleNotMatchingPasswordException(NotMatchingPasswordsException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(NotMatchingEmailsException.class)
    public ResponseEntity<ErrorMessageDto> handleNotMatchingEmailsException(NotMatchingEmailsException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ErrorMessageDto> handleUserAlreadyExistException(UserAlreadyExistException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(SameEmailException.class)
    public ResponseEntity<ErrorMessageDto> handleSameEmailException(SameEmailException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidEmailException(InvalidEmailException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleUserNotFoundException(UserNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(UserRoleNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleUserRoleNotFoundException(UserRoleNotFoundException exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<ErrorMessageDto> handleUserBannedException(UserBannedException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(UserAccountNotActivatedException.class)
    public ResponseEntity<ErrorMessageDto> handleUserAccountNotActivatedException(UserAccountNotActivatedException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }
}
