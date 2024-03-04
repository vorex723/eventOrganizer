package com.mazurek.eventOrganizer.exception.file;

public class FileTypeNotAllowedException extends RuntimeException {
    public FileTypeNotAllowedException() {
        super();
    }

    public FileTypeNotAllowedException(String message) {
        super(message);
    }

    public FileTypeNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileTypeNotAllowedException(Throwable cause) {
        super(cause);
    }

    protected FileTypeNotAllowedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
