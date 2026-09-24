package com.shopflow.catalog.service;


import com.shopflow.catalog.domain.model.*;
import com.shopflow.catalog.repository.ReservationRepository;
import com.shopflow.catalog.repository.StockItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationExpiryJobTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private StockItemRepository stockItemRepository;
    @Mock
    private Clock clock;

    @InjectMocks
    private ReservationExpiryJob expiryJob;

    @Test
    void expireStaleReservations_shouldReturnStock_forExpiredReservation(){
        Instant now = Instant.parse("2026-09-23T12:00:00Z");
        when(clock.instant()).thenReturn(now);

        Product product = new Product();
        product.setId(1L);
        Warehouse warehouse= new Warehouse();
        warehouse.setId(1L);

        Reservation reservation = new Reservation();
        reservation.setProduct(product);
        reservation.setWarehouse(warehouse);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setQuantity(3);
        reservation.setExpiresAt(now.minusSeconds(60));

        StockItem stockItem = new StockItem();
        stockItem.setQuantity(10);
        stockItem.setReservedQty(3);

        when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.PENDING),eq(now),any())).thenReturn(List.of(reservation));

        when(stockItemRepository.findByProductIdAndWarehouseId(1L,1L)).thenReturn(Optional.of(stockItem));

        expiryJob.expireStaleReservation();

        assertThat(stockItem.getReservedQty()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);



    }

}
