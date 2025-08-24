package org.nexo.authservice.exception;

import lombok.Data;

import java.util.Date;
@Data
public class ErrorReponse {
    private Date timestamp;
    private int status;
    private String path;
    private String error;
    private String message;

}
