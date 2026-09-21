package com.shopflow.catalog.web.dto;

import com.shopflow.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;

public record ProductRequest(
    // fields for CREATE
    String sku,
    String name,
    String description,
    Long categoryId,
    BigDecimal priceAmount,
    String priceCurrency,

    // fields for SEARCH
    String q,
    ProductStatus status,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Integer page,
    Integer size,
    String sort
) {
}
