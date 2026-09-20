package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.Warehouse;
import com.shopflow.catalog.web.dto.WarehouseResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {
    WarehouseResponse toResponse(Warehouse warehouse);
}
