package com.shopflow.catalog.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.catalog.config.ReservationRateLimitFilter;
import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.service.CategoryService;
import com.shopflow.catalog.web.dto.CategoryResponse;
import com.shopflow.catalog.web.dto.CreateCategoryRequest;
import org.apache.catalina.filters.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = CategoryController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {ReservationRateLimitFilter.class}
    )
)class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void create_shouldReturn201_whenValid() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("toys", "Toys", null);
        CategoryResponse response = new CategoryResponse(1L, "Toys", "toys", null, Collections.emptyList(), null, null);

        when(categoryService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("toys"));
    }

    @Test
    void create_shouldReturn400_whenSlugBlank() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("", "Toys", null);

        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void delete_shouldReturn409_whenCategoryHasChildren() throws Exception {
        doThrow(new ConflictException("CATEGORY_HAS_CHILDREN", "Cannot delete category with child categories"))
            .when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CATEGORY_HAS_CHILDREN"));
    }

    @Test
    void findById_shouldReturn200_whenExists() throws Exception {
        CategoryResponse response = new CategoryResponse(1L, "Electronics", "electronics", null, Collections.emptyList(), null, null);
        when(categoryService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/categories/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Electronics"));
    }

    private static org.mockito.stubbing.Stubber doThrow(RuntimeException ex) {
        return org.mockito.Mockito.doThrow(ex);
    }
}
