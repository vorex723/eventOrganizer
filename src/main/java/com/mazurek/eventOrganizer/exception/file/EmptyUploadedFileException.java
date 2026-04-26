package com.mazurek.eventOrganizer.exception.file;

public class EmptyUploadedFileException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Uploaded file is empty.";

    public EmptyUploadedFileException() {
        super(DEFAULT_MESSAGE);
    }

    public EmptyUploadedFileException(String message) {
        super(message);
    }

    public EmptyUploadedFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyUploadedFileException(Throwable cause) {
        super(cause);
    }

    protected EmptyUploadedFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
