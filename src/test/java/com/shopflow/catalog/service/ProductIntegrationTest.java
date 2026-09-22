package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.support.AbstractIntegrationTest;
import com.shopflow.catalog.web.dto.CreateProductRequest;
import com.shopflow.catalog.web.dto.ProductResponse;
import com.shopflow.catalog.web.dto.UpdateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProductIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ProductService productService;
    @Autowired private CategoryRepository categoryRepository;

    // create operation sends correct data
    public void create_sends_correct_data(){
        Category category = new Category();
        category.setName("Books");
        category.setSlug("TEST-BOOKS");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest request = new CreateProductRequest("TEST", "Test_Book","description", savedCategory.getId(),new BigDecimal("3.9"), "KWD");

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.sku()).isEqualTo("TEST");
        assertThat(response.createdAt()).isNotNull();
    }

    public void create_sku_duplicate_exception(){
        Category category = new Category();
        category.setName("Shoes");
        category.setSlug("TEST");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest request = new CreateProductRequest("DUPLICATE", "Test_Shoes","description", savedCategory.getId(),new BigDecimal("15.9"), "KWD");

        ProductResponse response = productService.create(request);

        assertThatThrownBy(()-> productService.create(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void update_shouldEvictCache_soSubsequentReadIsFresh() {
        // create + cache a product, update it, confirm the fresh value comes back
        Category category = new Category();
        category.setName("Cache Test");
        category.setSlug("cache-test-" + java.util.UUID.randomUUID());
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest request = new CreateProductRequest(
            "CACHE-TEST-01", "Original Name", "desc",
            savedCategory.getId(), new BigDecimal("10.000"), "KWD");
        ProductResponse created = productService.create(request);

        productService.findById(created.id()); // primes the cache

        UpdateProductRequest updateRequest = new UpdateProductRequest(
            "Updated Name", "desc", savedCategory.getId(),
            new BigDecimal("20.000"), "KWD");
        productService.update(created.id(), updateRequest); // evict cache

        ProductResponse afterUpdate = productService.findById(created.id()); // trigger cach
        assertThat(afterUpdate.name()).isEqualTo("Updated Name");
    }
}
