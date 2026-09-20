package com.shopflow.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
    @NotBlank
    @Size(max = 200)
    String name,

    String description,

    @NotNull Long categoryId,

    @NotNull
    @DecimalMin(value = "0.001", message = "price must be greater than zero")
    BigDecimal priceAmount,


    @NotBlank
    @Size(min = 3, max = 3)
    String priceCurrency
    ){
}
