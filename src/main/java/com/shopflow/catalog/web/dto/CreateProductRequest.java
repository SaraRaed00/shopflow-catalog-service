package com.shopflow.catalog.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(
// we add validation annotations for data coming IN from the client
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase letters, numbers, and hyphens")
    String sku,

    @NotBlank
    @Size(max = 100)
    String name,

    String description,

    @NotNull
    Long categoryId,

    @NotNull
    @DecimalMin(value = "0.001", message = "price must be greater than zero")
    BigDecimal priceAmount,

    @NotBlank
    @Size(min = 3, max = 3)
    String priceCurrency
) {
}
