package com.shopflow.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank @Size(max = 100) String slug,

    @NotBlank @Size(max = 100) String name,

    Long parentId



) {
}
