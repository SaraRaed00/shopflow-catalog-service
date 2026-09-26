package com.shopflow.catalog.web;
import com.shopflow.catalog.service.ReservationService;
import com.shopflow.catalog.web.dto.ApiError;
import com.shopflow.catalog.web.dto.CreateReservationRequest;
import com.shopflow.catalog.web.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@Tag(name = "Reservations", description = "Short-lived holds on stock during checkout")
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Create a reservation", description = "Holds stock for 15 minutes. Fails if not enough stock is available.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reservation created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "No stock record for this product/warehouse", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Insufficient available stock", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a reservation by reference")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation found"),
        @ApiResponse(responseCode = "404", description = "No reservation with this reference", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{ref}")
    public ReservationResponse findByReference(@PathVariable String ref) {
        return reservationService.findByReference(ref);
    }

    @Operation(summary = "Confirm a reservation", description = "Consumes the hold: physically deducts stock. Idempotent - calling twice has the same effect as once.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation confirmed"),
        @ApiResponse(responseCode = "404", description = "Reservation or stock record not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Illegal status transition", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{ref}/confirm")
    public ReservationResponse confirm(@PathVariable String ref) {

        return reservationService.confirm(ref);
    }

    @Operation(summary = "Release a reservation", description = "Returns the held stock without a sale. Idempotent - calling twice has the same effect as once.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation released"),
        @ApiResponse(responseCode = "404", description = "Reservation or stock record not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Illegal status transition", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{ref}/release")
    public ReservationResponse release(@PathVariable String ref) {

        return reservationService.release(ref);
    }
}
