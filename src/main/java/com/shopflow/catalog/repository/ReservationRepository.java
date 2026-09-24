package com.shopflow.catalog.repository;

import com.shopflow.catalog.domain.model.Reservation;
import com.shopflow.catalog.domain.model.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByReference(String reference);
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant instant, Pageable pageable);
}
