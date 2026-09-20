package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.Reservation;
import com.shopflow.catalog.web.dto.ReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    @Mapping(target = "productId" , source = "product.id")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    ReservationResponse toResponse(Reservation reservation);
}
