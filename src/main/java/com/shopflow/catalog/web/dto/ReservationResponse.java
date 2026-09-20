package com.shopflow.catalog.web.dto;

import java.time.Instant;

public record ReservationResponse (
    Long id,

    String reference,

    Long productId,

    String status,

    Long warehouseId,

    Integer quantity,

    Instant createdAt,

    Instant expiresAt
){
}
