package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationMapperTest {

    private final ReservationMapper mapper = new ReservationMapperImpl();

    @Test
    void toResponse_shouldMapProductAndWarehouseIds() {
        Product product = new Product();
        product.setId(2L);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(3L);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReference("ref-abc");
        reservation.setProduct(product);
        reservation.setWarehouse(warehouse);
        reservation.setQuantity(5);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setExpiresAt(Instant.parse("2026-01-01T10:15:00Z"));
        reservation.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));

        var response = mapper.toResponse(reservation);

        assertThat(response.reference()).isEqualTo("ref-abc");
        assertThat(response.productId()).isEqualTo(2L);
        assertThat(response.warehouseId()).isEqualTo(3L);
        assertThat(response.quantity()).isEqualTo(5);
        assertThat(response.status()).isEqualTo("PENDING");
    }
}
