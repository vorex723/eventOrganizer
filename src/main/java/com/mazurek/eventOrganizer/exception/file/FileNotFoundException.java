package com.mazurek.eventOrganizer.exception.file;

public class FileNotFoundException extends RuntimeException {

    private static final String MESSAGE = "There is no file with this id.";

    public FileNotFoundException() {
        super(MESSAGE);
    }

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNotFoundException(Throwable cause) {
        super(cause);
    }

    protected FileNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
