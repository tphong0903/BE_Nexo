package org.nexo.feedservice.exception;

public class GrpcFeedException extends RuntimeException {
    public GrpcFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
