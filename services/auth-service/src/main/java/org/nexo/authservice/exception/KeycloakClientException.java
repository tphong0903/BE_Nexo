package org.nexo.authservice.exception;

public class KeycloakClientException extends RuntimeException {
    private final int status;
    private final String error;

    public KeycloakClientException(int status, String error) {
        this.status = status;
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
