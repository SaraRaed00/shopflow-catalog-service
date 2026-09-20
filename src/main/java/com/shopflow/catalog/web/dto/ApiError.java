package com.shopflow.catalog.web.dto;

import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;

public record ApiError (
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    String traceId,
    List<FieldError> fieldErrors
    ){
    public record FieldError(
        String field,
        String message
    ){}
}
