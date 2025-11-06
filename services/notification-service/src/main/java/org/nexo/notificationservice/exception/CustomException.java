package org.nexo.notificationservice.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
    private HttpStatus status;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}
