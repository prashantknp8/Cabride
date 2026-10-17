package com.cabride.ride_service.Exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex
    ){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedRideAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedRideAccessException ex
    ){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidRideStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidRideStatusException ex
    ){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(ex.getMessage()));
    }

    @ExceptionHandler( MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex
    ){
        String message= ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error-> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex
    ){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(ex.getMessage()));
    }

    private ErrorResponse buildResponse(String message) {

        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }


}
