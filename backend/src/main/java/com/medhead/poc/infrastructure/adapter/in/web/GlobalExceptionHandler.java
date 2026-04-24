package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.application.dto.ErrorResponse;
import com.medhead.poc.domain.exception.HospitalNotFoundException;
import com.medhead.poc.domain.exception.NoBedAvailableException;
import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.exception.RoutingServiceUnavailableException;
import com.medhead.poc.domain.exception.SpecialtyNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Central error-to-HTTP translation (E1). Controllers stay free of inline
 * {@code try/catch}; expected failure modes are mapped here to a structured
 * {@link ErrorResponse} with a stable shape.
 *
 * <p>Validation failures surface as 400 with per-field {@code details};
 * domain-absence errors surface as 404 with a stable {@code code};
 * optimistic-lock exhaustion surfaces as 503; any unmapped exception is
 * smothered into a generic 500 that never exposes stack traces or internals
 * (E2). Authentication failures keep their legacy {@code Map} shape to
 * preserve the {@code $.error == "invalid_credentials"} contract existing
 * clients and tests already depend on.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "invalid_credentials",
                "message", "Invalid username or password"
        ));
    }

    @ExceptionHandler(NoBedAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNoBedAvailable(NoBedAvailableException exception) {
        return error(HttpStatus.NOT_FOUND, "NO_BEDS_AVAILABLE", exception.getMessage(), null);
    }

    @ExceptionHandler(HospitalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHospitalNotFound(HospitalNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", exception.getMessage(), null);
    }

    @ExceptionHandler(SpecialtyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSpecialtyNotFound(SpecialtyNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "SPECIALTY_NOT_FOUND", exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request body failed validation", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ErrorResponse.FieldError> details = exception.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request parameters failed validation", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable() {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or malformed", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "No handler for " + exception.getHttpMethod() + " " + exception.getResourcePath(),
                null);
    }

    @ExceptionHandler(OptimisticLockConflictException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock() {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "RESERVATION_CONFLICT",
                "Bed reservation could not complete after retries -- retry later",
                null);
    }

    @ExceptionHandler(RoutingServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleRoutingUnavailable() {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "ROUTING_UNAVAILABLE",
                "Routing service is unavailable -- retry later",
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", null);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status,
                                                       String code,
                                                       String message,
                                                       List<ErrorResponse.FieldError> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                details
        ));
    }

    private static ErrorResponse.FieldError toFieldError(FieldError error) {
        String message = error.getDefaultMessage();
        return new ErrorResponse.FieldError(error.getField(),
                message == null ? "invalid value" : message);
    }

    private static ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        String message = violation.getMessage();
        return new ErrorResponse.FieldError(field,
                message == null ? "invalid value" : message);
    }
}
