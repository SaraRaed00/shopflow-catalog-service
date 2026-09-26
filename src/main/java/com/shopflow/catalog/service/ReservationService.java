package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.Reservation;
import com.shopflow.catalog.domain.model.ReservationStatus;
import com.shopflow.catalog.domain.model.StockItem;
import com.shopflow.catalog.mapper.ReservationMapper;
import com.shopflow.catalog.repository.ReservationRepository;
import com.shopflow.catalog.repository.StockItemRepository;
import com.shopflow.catalog.web.dto.CreateReservationRequest;
import com.shopflow.catalog.web.dto.ReservationResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.swing.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final StockItemRepository stockItemRepository;
    private final ReservationMapper reservationMapper;
    private static final Duration HOLD_DURATION = Duration.ofMinutes(15);
    private final Clock clock;
    // we will use it to record the metric
    private final MeterRegistry meterRegistry;

    public ReservationService(ReservationRepository reservationRepository, StockItemRepository stockItemRepository, ReservationMapper reservationMapper, Clock clock, MeterRegistry meterRegistry){
        this.reservationRepository = reservationRepository;
        this.stockItemRepository = stockItemRepository;
        this.reservationMapper = reservationMapper;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Retryable(
        retryFor = OptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 25, multiplier = 2)
    )
    @Transactional
    public ReservationResponse create(CreateReservationRequest request){
        // need a counter of created reservations and a timer tagged by outcome
        Timer.Sample sample = Timer.start(meterRegistry);
        // starting the timer
        String outcome = "success";
        try {
            // check that the stock has a pair(product id, warehouse id) available
            StockItem stock = stockItemRepository.findByProductIdAndWarehouseId(request.productId(), request.warehouseId())
                .orElseThrow(() -> new NotFoundException("NO STOCK EXISTS", "NO STOCK ITEM WITH THIS PRODUCT ID: " + request.productId() + " AND WAREHOUSE ID: " + request.warehouseId()));

            // check the quantity
            int available = stock.getQuantity() - stock.getReservedQty();
            if (request.quantity() > available) {
                outcome = "insufficient_stock";
                throw new ConflictException("NO STOCK AVAILABLE", "Available stock " + available + " is less than requested " + request.quantity());
            }
            // update the stock and create the reservation
            stock.setReservedQty(stock.getReservedQty() + request.quantity());

            Reservation reservation = new Reservation();
            reservation.setWarehouse(stock.getWarehouse());
            reservation.setProduct(stock.getProduct());
            reservation.setReference(UUID.randomUUID().toString());
            reservation.setQuantity(request.quantity());
            //reservation.setExpiresAt(Instant.now().plus(HOLD_DURATION));
            reservation.setExpiresAt(Instant.now(clock).plus(HOLD_DURATION));

            Reservation savedReservation = reservationRepository.save(reservation);
            // counter name = reservations.created
            meterRegistry.counter("reservations.created", "outcome", "success").increment();

            return reservationMapper.toResponse(savedReservation);
        }
        catch (RuntimeException exception){
            if(!"insufficient_stock".equals(outcome))
                outcome = "error!";
            meterRegistry.counter("reservations.created","outcome", outcome).increment();
            throw exception;
        }
        finally {
            sample.stop(meterRegistry.timer("reservations.create.duration", "outcome", outcome));
        }

    }
    @Transactional(readOnly = true)
    public ReservationResponse findByReference(String reference) {
        return reservationMapper.toResponse(findEntityByReference(reference));
    }

    @Transactional
    public ReservationResponse confirm(String reference) {
        Reservation reservation = findEntityByReference(reference);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return reservationMapper.toResponse(reservation);
        }
        reservation.changeStatus(ReservationStatus.CONFIRMED);
        StockItem stock = stockItemRepository.findByProductIdAndWarehouseId(
                reservation.getProduct().getId(), reservation.getWarehouse().getId())
            .orElseThrow(() -> new NotFoundException("STOCK_NOT_FOUND", "Stock record missing"));
        stock.setQuantity(stock.getQuantity() - reservation.getQuantity());
        stock.setReservedQty(stock.getReservedQty() - reservation.getQuantity());
        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse release(String reference) {
        Reservation reservation = findEntityByReference(reference);
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return reservationMapper.toResponse(reservation); // idempotent
        }
        reservation.changeStatus(ReservationStatus.RELEASED);
        StockItem stock = stockItemRepository.findByProductIdAndWarehouseId(
                reservation.getProduct().getId(), reservation.getWarehouse().getId())
            .orElseThrow(() -> new NotFoundException("STOCK_NOT_FOUND", "Stock record missing"));
        stock.setReservedQty(stock.getReservedQty() - reservation.getQuantity());
        return reservationMapper.toResponse(reservation);
    }

    private Reservation findEntityByReference(String reference) {
        return reservationRepository.findByReference(reference)
            .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "No reservation with reference " + reference));
    }
}
