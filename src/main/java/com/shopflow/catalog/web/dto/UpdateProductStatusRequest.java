package com.shopflow.catalog.web.dto;

import com.shopflow.catalog.domain.model.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest (
    @NotNull ProductStatus status
){

}
