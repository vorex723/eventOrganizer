package com.mazurek.eventOrganizer.exception.file;

public class EmptyUploadedFileException extends RuntimeException {
    public EmptyUploadedFileException() {
        super();
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
