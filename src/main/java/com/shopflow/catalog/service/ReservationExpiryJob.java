package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.model.Reservation;
import com.shopflow.catalog.domain.model.ReservationStatus;
import com.shopflow.catalog.domain.model.StockItem;
import com.shopflow.catalog.repository.ReservationRepository;
import com.shopflow.catalog.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component // background task, not a service
@RequiredArgsConstructor
public class ReservationExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);
    private static final Integer BATCH_SIZE = 50; // only 50 reservations are processed at a time

    private final ReservationRepository reservationRepository;
    private final StockItemRepository stockItemRepository;
    private final Clock clock;

    @Scheduled(fixedDelay = 60000) // every 60sec run this method
    public void expireStaleReservation(){
        Instant now = Instant.now(clock);
        Pageable batch = PageRequest.of(0 , BATCH_SIZE);

        List<Reservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now, batch);

        for(Reservation reservation: expired){
            reservation.changeStatus(ReservationStatus.EXPIRED);

            StockItem stockItem = stockItemRepository.findByProductIdAndWarehouseId(reservation.getProduct().getId(),reservation.getWarehouse().getId()).orElse(null);

            if(stockItem != null)
                stockItem.setReservedQty(stockItem.getReservedQty()-reservation.getQuantity());
        }

        if(!expired.isEmpty())
            log.info("Expired {} stale Reservations ", expired.size());
    }

}
