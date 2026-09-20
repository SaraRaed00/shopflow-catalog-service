package com.shopflow.catalog.web.dto;

public record StockResponse(
    Long warehouseId,
    String warehouseCode,
    Integer quantity,
    Integer reservedQty,
    Integer available

) {

}
