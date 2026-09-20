package com.shopflow.catalog.web.dto;

import java.time.Instant;
import java.util.List;

public record CategoryResponse(
    Long id,
    String name,
    String slug,
    Long parentId,
    List<CategoryResponse> children,
    Instant createdAt,
    Instant updatedAt
) {
}
