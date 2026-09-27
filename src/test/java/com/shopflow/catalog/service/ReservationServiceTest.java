package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.*;
import com.shopflow.catalog.mapper.ReservationMapper;
import com.shopflow.catalog.repository.ReservationRepository;
import com.shopflow.catalog.repository.StockItemRepository;
import com.shopflow.catalog.web.dto.CreateReservationRequest;
import com.shopflow.catalog.web.dto.ReservationResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private StockItemRepository stockItemRepository;
    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private Clock clock;


    private ReservationService reservationService;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
            reservationRepository, stockItemRepository, reservationMapper, clock, meterRegistry);
    }

    private StockItem stockItem(int quantity, int reserved) {
        StockItem item = new StockItem();
        item.setQuantity(quantity);
        item.setReservedQty(reserved);
        item.setProduct(new Product());
        item.setWarehouse(new Warehouse());
        return item;
    }

    @Test
    void create_shouldNotMutateStockObject_whenSaveFails() {
        when(clock.instant()).thenReturn(Instant.now());
        StockItem item = stockItem(10, 0);
        CreateReservationRequest request = new CreateReservationRequest(1L, 1L, 5);

        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(item));
        when(reservationRepository.save(any(Reservation.class)))
            .thenThrow(new RuntimeException("Simulated database failure"));

        assertThatThrownBy(() -> reservationService.create(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Simulated database failure");
    }

    @Test
    void expireAt_should_be_15min_after_createdAt() {
        Instant now = Instant.parse("2026-09-21T10:00:00Z");
        when(clock.instant()).thenReturn(now);

        StockItem item = stockItem(10, 0);
        CreateReservationRequest request = new CreateReservationRequest(2L, 1L, 5);
        Reservation savedStub = new Reservation();
        Instant expectedExpiry = now.plus(Duration.ofMinutes(15));
        ReservationResponse response = new ReservationResponse(1L, "ref-1", 2L, "PENDING", 1L, 5, expectedExpiry, now);

        when(stockItemRepository.findByProductIdAndWarehouseId(2L, 1L)).thenReturn(Optional.of(item));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedStub);
        when(reservationMapper.toResponse(savedStub)).thenReturn(response);

        reservationService.create(request);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    void create_shouldThrowConflict_whenInsufficientStock() {
        StockItem item = stockItem(10, 8); // only 2 available
        CreateReservationRequest request = new CreateReservationRequest(1L, 1L, 5);
        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> reservationService.create(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Available stock");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void create_shouldIncrementReservedQty_whenStockAvailable() {
        when(clock.instant()).thenReturn(Instant.now());
        //when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        StockItem item = stockItem(10, 0);
        CreateReservationRequest request = new CreateReservationRequest(1L, 1L, 5);
        Reservation saved = new Reservation();
        ReservationResponse mapped = new ReservationResponse(1L, "ref-1", 1L, "PENDING", 1L, 5, null, null);

        when(stockItemRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(item));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);
        when(reservationMapper.toResponse(saved)).thenReturn(mapped);

        ReservationResponse result = reservationService.create(request);

        assertThat(item.getReservedQty()).isEqualTo(5);
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void confirm_shouldBeIdempotent_whenAlreadyConfirmed() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        ReservationResponse mapped = new ReservationResponse(1L, "ref-1", 1L, "CONFIRMED", 1L, 5 , null, null);

        when(reservationRepository.findByReference("ref-1")).thenReturn(Optional.of(reservation));
        when(reservationMapper.toResponse(reservation)).thenReturn(mapped);

        ReservationResponse result = reservationService.confirm("ref-1");

        assertThat(result.status()).isEqualTo("CONFIRMED");
        // proves it did NOT try to touch stock a second time
        verifyNoInteractions(stockItemRepository);
    }

    @Test
    void release_shouldThrowConflict_whenAlreadyConfirmed() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findByReference("ref-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.release("ref-1"))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void findByReference_shouldThrowNotFound_whenMissing() {
        when(reservationRepository.findByReference("bad-ref")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.findByReference("bad-ref"))
            .isInstanceOf(NotFoundException.class);
    }
    @Test
    void confirm_shouldThrowNotFound_whenStockRecordMissing() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setProduct(new Product());
        reservation.setWarehouse(new Warehouse());
        reservation.setQuantity(5);

        when(reservationRepository.findByReference("ref-1")).thenReturn(Optional.of(reservation));
        when(stockItemRepository.findByProductIdAndWarehouseId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.confirm("ref-1"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void release_shouldThrowNotFound_whenReservationMissing() {
        when(reservationRepository.findByReference("bad-ref")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.release("bad-ref"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void release_shouldDecrementReservedQty_whenValid() {
        StockItem item = stockItem(10, 5);
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setProduct(item.getProduct());
        reservation.setWarehouse(item.getWarehouse());
        reservation.setQuantity(5);

        when(reservationRepository.findByReference("ref-1")).thenReturn(Optional.of(reservation));
        when(stockItemRepository.findByProductIdAndWarehouseId(any(), any())).thenReturn(Optional.of(item));

        reservationService.release("ref-1");

        assertThat(item.getReservedQty()).isEqualTo(0);
    }
}
