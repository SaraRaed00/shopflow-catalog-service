package com.shopflow.catalog.web.dto;

import com.shopflow.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;

public record ProductSearchCriteria (
    String q,
    Long categoryId,
    ProductStatus status,
    BigDecimal minPrice,
    BigDecimal maxPrice
){
}
