package org.nexo.feedservice.exception;

public class DatabaseFeedException extends RuntimeException {
    public DatabaseFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
