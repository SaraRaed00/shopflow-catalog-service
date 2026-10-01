package com.shopflow.catalog.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.catalog.config.ReservationRateLimitFilter;
import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.service.InventoryService;
import com.shopflow.catalog.web.dto.AdjustStockRequest;
import org.apache.catalina.filters.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = InventoryController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {ReservationRateLimitFilter.class}
    )
)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void adjust_shouldReturn200_whenValid() throws Exception {
        AdjustStockRequest request = new AdjustStockRequest(1L, 1L, 10, "restock");

        mockMvc.perform(post("/api/v1/inventory/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void adjust_shouldReturn409_whenInsufficientStock() throws Exception {
        AdjustStockRequest request = new AdjustStockRequest(1L, 1L, -100, "damage");
        doThrow(new ConflictException("INSUFFICIENT_STOCK", "Adjustment would drop below reserved"))
            .when(inventoryService).adjust(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/inventory/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void adjust_shouldReturn400_whenReasonMissing() throws Exception {
        String badJson = "{\"productId\":1,\"warehouseId\":1,\"quantityChange\":10}";

        mockMvc.perform(post("/api/v1/inventory/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
            .andExpect(status().isBadRequest());
    }
}
