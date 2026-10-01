package com.shopflow.catalog.web.dto;

import java.util.List;

public record keysetPageResponse<T>(
    List<T> content,
    Long nextCursor,
    boolean hasNext

) {

}
