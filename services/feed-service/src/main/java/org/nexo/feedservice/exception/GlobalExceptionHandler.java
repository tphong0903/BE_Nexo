package org.nexo.feedservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.nexo.feedservice.dto.ResponseData;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Date;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            IllegalArgumentException.class, PropertyReferenceException.class})
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
        } else if (e instanceof ConstraintViolationException || e instanceof IllegalArgumentException
                || e instanceof PropertyReferenceException) {
            message = message.substring(message.indexOf(" ") + 1);
            errorResponse.setError("PathVariable Invalid");

        }
        errorResponse.setMessage(message);

        return errorResponse;
    }

    @ExceptionHandler(TransactionSystemException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTransactionSystemException(TransactionSystemException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Validation Failed");

        // Extract the root cause to get the actual validation message
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof jakarta.validation.ConstraintViolationException) {
                jakarta.validation.ConstraintViolationException cve = (jakarta.validation.ConstraintViolationException) cause;
                String violations = cve.getConstraintViolations().stream()
                        .map(violation -> violation.getMessage())
                        .collect(java.util.stream.Collectors.joining(", "));
                errorResponse.setMessage(violations);
                return errorResponse;
            }
            cause = cause.getCause();
        }

        // Fallback message if no ConstraintViolationException found
        errorResponse.setMessage("Transaction failed due to validation error");
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseData<?> handleNotFoundException(ResourceNotFoundException ex) {
        return new ResponseData<>(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseData<?> handleDataConflict(DataIntegrityViolationException ex) {
        return new ResponseData<>(HttpStatus.CONFLICT.value(),
                "Database constraint violation: " + ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseData<?> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {} - Path: {}", ex.getMessage(), request.getDescription(false));
        return ResponseData.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message("Access denied: You don't have permission to access this resource")
                .build();
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ResponseData<?>> handleCustomException(CustomException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.UNPROCESSABLE_ENTITY;
        ResponseData<?> response = new ResponseData<>(
                status.value(),
                ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }
}
