package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.domain.model.Product;
import com.shopflow.catalog.mapper.ProductMapper;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.web.dto.CreateProductRequest;
import com.shopflow.catalog.web.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) - activates Mockito's annotations below.
// NOTE: no Spring, no database, no @SpringBootTest - this is pure Java + Mockito,
// which is exactly why it's the fastest kind of test (milliseconds, not seconds).
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    // @Mock creates a FAKE ProductRepository - not connected to any real database.
    // You control exactly what it returns in each test.
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    // @InjectMocks creates a REAL ProductService, but automatically plugs in
    // the three fakes above wherever the constructor asks for them.
    @InjectMocks
    private ProductService productService;

    @Test
    void create_shouldThrowConflict_whenSkuAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest(
            "DUPLICATE-SKU", "Some Product", "desc", 1L,
            new BigDecimal("9.999"), "KWD");

        // "when someone calls existsBySku with this exact SKU, pretend it returns true"
        when(productRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("DUPLICATE-SKU");

        // The real assertion: prove the code NEVER got as far as trying to save.
        // This is what your guide means by "asserts on behaviour, not merely
        // that a mock was called" - we're checking save was correctly AVOIDED.
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowNotFound_whenCategoryDoesNotExist() {
        CreateProductRequest request = new CreateProductRequest(
            "NEW-SKU", "Some Product", "desc", 999L,
            new BigDecimal("9.999"), "KWD");

        when(productRepository.existsBySku("NEW-SKU")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
            .isInstanceOf(NotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void create_shouldSaveAndReturnResponse_whenValid() {
        CreateProductRequest request = new CreateProductRequest(
            "NEW-SKU", "Some Product", "desc", 1L,
            new BigDecimal("9.999"), "KWD");
        Category category = new Category();
        Product savedProduct = new Product();
        ProductResponse expectedResponse = new ProductResponse(
            1L, "NEW-SKU", "Some Product", "desc", 1L, "Electronics",
            new BigDecimal("9.999"), "KWD", "DRAFT", null, null);

        when(productRepository.existsBySku("NEW-SKU")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toResponse(savedProduct)).thenReturn(expectedResponse);

        ProductResponse result = productService.create(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void findById_shouldThrowNotFound_whenProductMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(42L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("42");
    }
}
