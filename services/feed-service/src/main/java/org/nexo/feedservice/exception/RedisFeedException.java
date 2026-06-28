package org.nexo.feedservice.exception;

public class RedisFeedException extends RuntimeException {
    public RedisFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
