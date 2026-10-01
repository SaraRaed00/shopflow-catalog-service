package com.shopflow.catalog.web.dto;

import java.time.Instant;

public record ReservationConfirmedPayload(
    String reference,
    Long productId,
    Long warehouseId,
    int quantity,
    Instant confirmedAt


) {
}
