package com.shopflow.catalog.web.dto;

import java.util.List;

public record ImportReport (
    long totalRows,
    long imported,
    long failedInvalidRowException,
    long failedConflictException,
    long failedNotFoundException,
    long failedDataIntegrity,
    List<ImportRowError> errors
){
}
