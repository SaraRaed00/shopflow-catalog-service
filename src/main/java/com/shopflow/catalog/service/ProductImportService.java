package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.InvalidRequestException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.web.dto.CreateProductRequest;
import com.shopflow.catalog.web.dto.ImportReport;
import com.shopflow.catalog.web.dto.ImportRowError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private static final List<String> REQUIRED_HEADERS = List.of("sku", "name", "description", "categoryId", "priceAmount", "priceCurrency");
    private static final int MAX_REPORTED_ERRORS = 100;

    private final ProductService productService;
    private final Validator validator;

    public ImportReport importCsv(InputStream input) throws IOException {
        List<ImportRowError> errors = new ArrayList<>();
        long total = 0;
        long imported = 0;
        long failedInvalidRowException = 0;
        long failedConflictException = 0;
        long failedNotFoundException =0;
        long failedDataIntegrity = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build();

        try (Reader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             CSVParser parser = format.parse(reader)) {

            checkHeaders(parser);

            for (CSVRecord record : parser) {
                total++;
                try {
                    CreateProductRequest request = toRequest(record);
                    checkConstraints(request);
                    productService.create(request);
                    imported++;
                } catch (InvalidRowException ex) {
                    failedInvalidRowException++;
                    addError(errors, record, ex.code, ex.getMessage());
                } catch (ConflictException ex) {
                    failedConflictException++;
                    addError(errors, record, ex.getCode(), ex.getMessage());
                } catch (NotFoundException ex) {
                    failedNotFoundException++;
                    addError(errors, record, ex.getCode(), ex.getMessage());
                } catch (DataIntegrityViolationException ex) {
                    failedDataIntegrity++;
                    addError(errors, record, "CONSTRAINT_VIOLATION", "Row violates a database constraint");
                }
            }
        }
        return new ImportReport(total, imported, failedInvalidRowException,failedConflictException,failedNotFoundException,failedDataIntegrity,errors);
    }





    private void checkHeaders(CSVParser parser) {
        Set<String> present = parser.getHeaderMap().keySet();
        List<String> missing = REQUIRED_HEADERS.stream().filter(header -> !present.contains(header)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("INVALID_CSV_HEADER", "Missing columns: " + missing);
        }
    }

    private CreateProductRequest toRequest(CSVRecord r) {
        if (!r.isConsistent()) {
            throw new InvalidRowException("WRONG_COLUMN_COUNT",
                "Expected " + REQUIRED_HEADERS.size() + " columns but found " + r.size());
        }
        try {
            return new CreateProductRequest(
                r.get("sku"), r.get("name"), r.get("description"),
                Long.valueOf(r.get("categoryId")),
                new BigDecimal(r.get("priceAmount")),
                r.get("priceCurrency"));
        } catch (NumberFormatException ex) {
            throw new InvalidRowException("INVALID_NUMBER", "categoryId or priceAmount is not a valid number");
        }
    }

    private void checkConstraints(CreateProductRequest request) {
        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
            throw new InvalidRowException("VALIDATION_FAILED", message);
        }
    }

    private void addError(List<ImportRowError> errors, CSVRecord r, String code, String message) {
        if (errors.size() < MAX_REPORTED_ERRORS) {
            String sku = r.isMapped("sku") && r.isSet("sku") ? r.get("sku") : null;
            errors.add(new ImportRowError(r.getRecordNumber(), sku, code, message));
        }
    }

    private static class InvalidRowException extends RuntimeException {
        final String code;
        InvalidRowException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
