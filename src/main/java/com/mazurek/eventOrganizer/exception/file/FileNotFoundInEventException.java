package com.mazurek.eventOrganizer.exception.file;

public class FileNotFoundInEventException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "There is no file with this id in given event.";

    public FileNotFoundInEventException() {
        super(DEFAULT_MESSAGE);
    }

    public FileNotFoundInEventException(String message) {
        super(message);
    }

    public FileNotFoundInEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNotFoundInEventException(Throwable cause) {
        super(cause);
    }
}
