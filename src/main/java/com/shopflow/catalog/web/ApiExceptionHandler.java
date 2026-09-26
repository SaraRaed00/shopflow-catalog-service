package com.shopflow.catalog.web;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.InvalidRequestException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.web.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// @RestControllerAdvice means: watch every controller in the whole app.
// This is the ONE place in the entire app that decides how errors look to the client.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // triggered whenever ANY controller method (via its service) throws NotFoundException
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        // ex = the actual exception object that was thrown, so we can read its code/message
        // req = the original HTTP request, so we can read which URL was being called (for the "path" field)
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), req, null);
        // 404 - matches your guide's status table: "The addressed resource does not exist"
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), req, null);
        // 409 - "Rule violation: duplicate SKU, illegal transition, not enough stock..."
    }

    // This one is special: MethodArgumentNotValidException is NOT something we throw ourselves.
    // Spring throws it AUTOMATICALLY when a @Valid @RequestBody fails its Bean Validation, checks (@NotBlank, @Positive, etc.) - e.g. someone POSTs a product with a blank name.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // ex.getBindingResult().getFieldErrors() = the list of every field that failed validation, each one knowing which field it was and what the validation message should be.
        // We convert Spring's internal FieldError objects into OUR OWN ApiError.FieldError shape -
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request failed validation", req, errors);
        // 400, with the actual list of what was wrong attached in fieldErrors -
        // matches "A validation failure returns 400 with one entry per invalid field."
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    // for violating constraints,...
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "The request conflicts with existing data", req, null);
        // 409 - same status family as ConflictException, since it's the same KIND of
        // problem (a rule violation), just caught one layer deeper than usual.
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body could not be parsed", req, null);
    }

    // The catch-all - matches Exception.class, meaning literally anything not already handled by a more specific @ExceptionHandler above. This is our safety net so that NOTHING ever leaks a raw Java stack trace to a client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req, null);
    }
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), req, null);
    }

    // Every handler above calls this to actually construct the response.
    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest req, List<ApiError.FieldError> fieldErrors) {
        String traceId = org.slf4j.MDC.get("traceId");
        ApiError error = new ApiError(
            Instant.now(),              // timestamp - exactly when this error happened
            status.value(),             // the numeric status code, e.g. 404 (status itself is an enum-like object; .value() gets the plain int)
            code,                       // our machine-readable code, e.g. "PRODUCT_NOT_FOUND" - what clients should branch on
            message,                    // human-readable explanation - can change wording anytime, never parsed by code
            req.getRequestURI(),        // which URL was being called, e.g. "/api/v1/products/42"
            traceId,
            // useful for matching a client's bug report to your server logs
            fieldErrors                 // null for most errors; only populated for validation failures
        );
        return ResponseEntity.status(status).body(error);
        // Same ResponseEntity pattern as your controllers - just building a response
        // with a specific status and this ApiError object as the JSON body.
    }
}
