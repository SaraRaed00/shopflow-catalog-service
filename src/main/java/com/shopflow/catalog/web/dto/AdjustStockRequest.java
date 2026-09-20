package com.shopflow.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdjustStockRequest (
    @NotNull Long warehouseId,

    @NotNull Long productId,

    @NotNull Integer quantityChange, //could be add or remove

    @NotBlank String reason
){
}
