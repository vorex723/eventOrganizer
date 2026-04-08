package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FileExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(EmptyUploadedFileException.class)
    public ResponseEntity<ErrorMessageDto> handleEmptyUploadedFileException(EmptyUploadedFileException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(FileTypeNotAllowedException.class)
    public ResponseEntity<ErrorMessageDto> handleFileTypeNotAllowedException(FileTypeNotAllowedException exception) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleFileNotFoundException(FileNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(FileNotFoundInEventException.class)
    public ResponseEntity<ErrorMessageDto> handleFileNotFoundInEventException(FileNotFoundInEventException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }
}
