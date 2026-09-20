package com.shopflow.catalog.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferStockRequest(
    @NotNull Long productId,

    @NotNull Long fromWarehouseId,

    @NotNull Long toWarehouseId,

    @NotNull @Positive Integer quantity

    ){
}
