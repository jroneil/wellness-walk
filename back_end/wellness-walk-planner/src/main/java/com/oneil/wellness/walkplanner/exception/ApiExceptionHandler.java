package com.oneil.wellness.walkplanner.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.oneil.wellness.walkplanner.dto.ApplicationErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApplicationErrorResponse> handleDomain(RuntimeException exception) {
        return ResponseEntity.badRequest().body(new ApplicationErrorResponse(400, "Bad Request", exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApplicationErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return ResponseEntity.status(status)
                .body(new ApplicationErrorResponse(status.value(), status.getReasonPhrase(), message));
    }
}
