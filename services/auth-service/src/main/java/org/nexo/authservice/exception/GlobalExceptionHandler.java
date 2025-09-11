package org.nexo.authservice.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.nexo.authservice.dto.ResponseData;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler({ MethodArgumentNotValidException.class, ConstraintViolationException.class,
            IllegalArgumentException.class, PropertyReferenceException.class })
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

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class })
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
    public ResponseEntity<String> handleNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseData<?> handleDataConflict(DataIntegrityViolationException ex) {
        return new ResponseData<>(HttpStatus.CONFLICT.value(),
                "Database constraint violation: " + ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(KeycloakClientException.class)
    public ResponseEntity<Map<String, Object>> handleKeycloakException(KeycloakClientException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatus());
        body.put("error", ex.getError());
        return ResponseEntity.status(HttpStatus.valueOf(ex.getStatus())).body(body);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ResponseData<Object>> handleWebClientException(WebClientResponseException ex) {
        log.error("WebClient error: Status={}, Response={}", ex.getStatusCode(), ex.getResponseBodyAsString());

        try {
            String responseBody = ex.getResponseBodyAsString();
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> errorMap = mapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {
                    });

            if (errorMap != null) {
                String errorDescription = (String) errorMap.get("error_description");
                String error = (String) errorMap.get("error");
                String message = errorDescription != null ? errorDescription : error;

                ResponseData<Object> errorResponse = ResponseData.builder()
                        .status(ex.getStatusCode().value())
                        .message(message != null ? message : "External service error")
                        .build();

                return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
            }
        } catch (Exception e) {
            log.warn("Could not parse error response: {}", ex.getResponseBodyAsString());
        }

        ResponseData<Object> errorResponse = ResponseData.builder()
                .status(ex.getStatusCode().value())
                .message("External service error")
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

}
