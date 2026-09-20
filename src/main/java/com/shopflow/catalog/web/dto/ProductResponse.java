package com.shopflow.catalog.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String description,
    Long categoryId,
    String categoryName,
    BigDecimal priceAmount,
    String priceCurrency,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
}
