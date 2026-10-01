package com.shopflow.catalog.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.catalog.config.ReservationRateLimitFilter;
import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.service.ReservationService;
import com.shopflow.catalog.web.dto.CreateReservationRequest;
import com.shopflow.catalog.web.dto.ReservationResponse;
import org.apache.catalina.filters.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ReservationController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {ReservationRateLimitFilter.class}
    )
)class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void create_shouldReturn201_whenValid() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(1L, 1L, 5);
        ReservationResponse response = new ReservationResponse(1L, "ref-1", 1L, "PENDING", 1L, 5, null, null);
        when(reservationService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reference").value("ref-1"));
    }

    @Test
    void create_shouldReturn409_whenInsufficientStock() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(1L, 1L, 1000);
        when(reservationService.create(any()))
            .thenThrow(new ConflictException("INSUFFICIENT_STOCK", "Not enough available stock"));

        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void confirm_shouldReturn200() throws Exception {
        ReservationResponse response = new ReservationResponse(1L, "ref-1", 1L, "CONFIRMED", 1L, 5, null, null);
        when(reservationService.confirm("ref-1")).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations/ref-1/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void release_shouldReturn409_whenIllegalTransition() throws Exception {
        when(reservationService.release("ref-1"))
            .thenThrow(new ConflictException("ILLEGAL_STATUS_TRANSITION", "Cannot move from CONFIRMED to RELEASED"));

        mockMvc.perform(post("/api/v1/reservations/ref-1/release"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ILLEGAL_STATUS_TRANSITION"));
    }
}
