package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.domain.model.Product;
import com.shopflow.catalog.domain.model.ProductStatus;
import com.shopflow.catalog.mapper.ProductMapper;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.web.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
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

    ////

    @Test
    void findBySku_shouldReturnResponse_whenExists() {
        Product product = new Product();
        ProductResponse response = new ProductResponse(1L, "SKU-1", "Name", "desc", 1L, "Cat",
            new BigDecimal("5.000"), "KWD", "DRAFT", null, null);
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.findBySku("SKU-1");

        assertThat(result.sku()).isEqualTo("SKU-1");
    }

    @Test
    void findBySku_shouldThrowNotFound_whenMissing() {
        when(productRepository.findBySku("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findBySku("MISSING"))
            .isInstanceOf(NotFoundException.class);

    }

    @Test
    void update_shouldModifyAndReturnProduct_whenValid() {
        Product product = new Product();
        Category category = new Category();
        UpdateProductRequest request = new UpdateProductRequest("New Name", "New Desc", 2L,
            new BigDecimal("20.000"), "KWD");
        ProductResponse response = new ProductResponse(1L, "SKU-1", "New Name", "New Desc", 2L, "Cat2",
            new BigDecimal("20.000"), "KWD", "DRAFT", null, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.update(1L, request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(product.getName()).isEqualTo("New Name");
    }

    @Test
    void update_shouldThrowNotFound_whenCategoryMissing() {
        Product product = new Product();
        UpdateProductRequest request = new UpdateProductRequest("Name", "desc", 999L,
            new BigDecimal("10.000"), "KWD");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(1L, request))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void changeStatus_shouldUpdateStatus_whenTransitionLegal() {
        Product product = new Product(); //DRAFT
        ProductResponse response = new ProductResponse(1L, "SKU-1", "Name", "desc", 1L, "Cat",
            new BigDecimal("5.000"), "KWD", "ACTIVE", null, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.changeStatus(1L, ProductStatus.ACTIVE);

        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void changeStatus_shouldThrowConflict_whenTransitionIllegal() {
        Product product = new Product();
        product.setStatus(ProductStatus.DISCONTINUED);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.changeStatus(1L, ProductStatus.ACTIVE))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void search_shouldReturnPagedResults() {
        Product product = new Product();
        ProductResponse response = new ProductResponse(1L, "SKU-1", "Name", "desc", 1L, "Cat", new BigDecimal("5.000"), "KWD", "DRAFT", null, null);
        Page<Product> page = new PageImpl<>(List.of(product));
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, null, null, null);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        PageResponse<ProductResponse> result = productService.search(criteria, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
    }
}
