package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.web.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toResponse(Category category);
}
