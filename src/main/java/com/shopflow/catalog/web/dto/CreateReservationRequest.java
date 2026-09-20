package com.shopflow.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
    @NotNull Long productId,

    @NotNull Long warehouseId,

    @NotNull @Positive Integer quantity



) {
}
