package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.Product;
import com.shopflow.catalog.web.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId" , source = "category.id")
    @Mapping(target = "categoryName" , source = "category.name")
    @Mapping(target = "priceAmount", source = "price.amount")
    @Mapping(target = "priceCurrency", source = "price.currency")
    ProductResponse toResponse(Product product);
}
