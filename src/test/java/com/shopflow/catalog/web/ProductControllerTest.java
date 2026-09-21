package com.shopflow.catalog.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.service.InventoryService;
import com.shopflow.catalog.service.ProductService;
import com.shopflow.catalog.web.dto.CreateProductRequest;
import com.shopflow.catalog.web.dto.PageResponse;
import com.shopflow.catalog.web.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest starts ONLY the web layer for this one controller - routing,
// validation, JSON serialization, and your ApiExceptionHandler. No database,
// no real ProductService - much faster than a full @SpringBootTest.
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc; // lets us "send" fake HTTP requests without a real server

    private final ObjectMapper objectMapper = new ObjectMapper();

    // @MockBean = a fake ProductService, registered into Spring's context for
    // this test only. The controller gets this fake instead of the real one.
    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private InventoryService inventoryService; // controller needs this too, even unused here

    @Test
    void create_shouldReturn201_whenValid() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
            "TEST-SKU", "Test Product", "desc", 1L,
            new BigDecimal("5.000"), "KWD");
        ProductResponse response = new ProductResponse(
            1L, "TEST-SKU", "Test Product", "desc", 1L, "Electronics",
            new BigDecimal("5.000"), "KWD", "DRAFT", null, null);

        when(productService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/products/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("TEST-SKU"));
    }

    @Test
    void create_shouldReturn400_whenSkuIsBlank() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
            "", "Test Product", "desc", 1L, new BigDecimal("5.000"), "KWD");

        mockMvc.perform(post("/api/v1/products/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("sku"));
    }

    @Test
    void create_shouldReturn409_whenDuplicateSku() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
            "DUP-SKU", "Test Product", "desc", 1L, new BigDecimal("5.000"), "KWD");

        when(productService.create(any()))
            .thenThrow(new ConflictException("DUPLICATE_SKU", "SKU already exists"));

        mockMvc.perform(post("/api/v1/products/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_SKU"));
    }

    @Test
    void findById_shouldReturn200_whenProductExists() throws Exception {
        ProductResponse response = new ProductResponse(
            5L, "SKU-5", "Product Five", "desc", 1L, "Electronics",
            new BigDecimal("10.000"), "KWD", "ACTIVE", null, null);
        when(productService.findById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    @Test
    void search_shouldApplyMultipleFilters_whenCombined() throws Exception {
        PageResponse<ProductResponse> emptyPage = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(productService.search(any(), any())).thenReturn(emptyPage);

        // combination 1: text + status
        mockMvc.perform(post("/api/v1/products/search?q=widget&status=ACTIVE"))
            .andExpect(status().isOk());

        // combination 2: category + price range
        mockMvc.perform(post("/api/v1/products/search?categoryId=1&minPrice=5&maxPrice=50"))
            .andExpect(status().isOk());

        // combination 3: status only
        mockMvc.perform(post("/api/v1/products/search?status=DRAFT"))
            .andExpect(status().isOk());

        // combination 4: no filters at all
        mockMvc.perform(post("/api/v1/products/search"))
            .andExpect(status().isOk());
    }

    @Test
    void search_shouldReturn400_whenSortFieldInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/products/search?sort=badField"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }
}
