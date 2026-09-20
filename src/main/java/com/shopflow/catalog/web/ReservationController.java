package com.shopflow.catalog.web;
import com.shopflow.catalog.service.ReservationService;
import com.shopflow.catalog.web.dto.CreateReservationRequest;
import com.shopflow.catalog.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ref}")
    public ReservationResponse findByReference(@PathVariable String ref) {
        return reservationService.findByReference(ref);
    }

    @PostMapping("/{ref}/confirm")
    public ReservationResponse confirm(@PathVariable String ref) {
        return reservationService.confirm(ref);
    }

    @PostMapping("/{ref}/release")
    public ReservationResponse release(@PathVariable String ref) {
        return reservationService.release(ref);
    }
}
