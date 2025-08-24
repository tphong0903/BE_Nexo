package org.nexo.uploadfileservice.exception;


import org.nexo.uploadfileservice.dto.response.ResponseData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class,
            IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlerValidationException(Exception e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        String message = e.getMessage();
        if (e instanceof MethodArgumentNotValidException) {
            int start = message.lastIndexOf("[");
            int end = message.lastIndexOf("]");
            message = message.substring(start + 1, end - 1);
            errorResponse.setError("Payload Invalid");
        } else if ( e instanceof IllegalArgumentException n) {
            message = message.substring(message.indexOf(" ") + 1);
            errorResponse.setError("PathVariable Invalid");

        }
        errorResponse.setMessage(message);

        return errorResponse;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handlerInternalServerException(Exception e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        if (e instanceof MethodArgumentTypeMismatchException)
            errorResponse.setMessage("Failed to convert value of type");

        return errorResponse;
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ResponseData<?>> handleCustomException(CustomException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.UNPROCESSABLE_ENTITY;
        ResponseData<?> response = new ResponseData<>(
                status.value(),
                ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseData<?> handleUnexpected(Exception ex) {
        return new ResponseData<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error: " + ex.getMessage());
    }
}
