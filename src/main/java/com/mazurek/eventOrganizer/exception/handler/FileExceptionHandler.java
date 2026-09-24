package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.file.EventFileQuotaExceededException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FileExceptionHandler extends BaseDomainExceptionHandler {

    private static final String FILE_TOO_LARGE_MESSAGE = "Uploaded file exceeds the maximum allowed size.";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorMessageDto> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception
    ) {
        return buildErrorResponse(HttpStatus.CONTENT_TOO_LARGE, ApiErrorCode.FILE_TOO_LARGE, FILE_TOO_LARGE_MESSAGE);
    }

    @ExceptionHandler(EmptyUploadedFileException.class)
    public ResponseEntity<ErrorMessageDto> handleEmptyUploadedFileException(EmptyUploadedFileException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.EMPTY_UPLOADED_FILE, exception);
    }

    @ExceptionHandler(FileTypeNotAllowedException.class)
    public ResponseEntity<ErrorMessageDto> handleFileTypeNotAllowedException(FileTypeNotAllowedException exception) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ApiErrorCode.FILE_TYPE_NOT_ALLOWED, exception);
    }

    @ExceptionHandler(EventFileQuotaExceededException.class)
    public ResponseEntity<ErrorMessageDto> handleEventFileQuotaExceededException(EventFileQuotaExceededException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, ApiErrorCode.EVENT_FILE_QUOTA_EXCEEDED, exception);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleFileNotFoundException(FileNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ApiErrorCode.FILE_NOT_FOUND, exception);
    }

    @ExceptionHandler(FileNotFoundInEventException.class)
    public ResponseEntity<ErrorMessageDto> handleFileNotFoundInEventException(FileNotFoundInEventException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ApiErrorCode.FILE_NOT_FOUND_IN_EVENT, exception);
    }
}
