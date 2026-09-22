package com.shopflow.catalog.Repository;

import com.shopflow.catalog.config.JpaConfig;
import com.shopflow.catalog.domain.model.*;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.repository.ProductRepository;
import com.shopflow.catalog.repository.ProductSpecification;
import com.shopflow.catalog.web.dto.ProductSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ProductSpecificationTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Spec Test Category");
        category.setSlug("spec-test-" + System.nanoTime());
        entityManager.persist(category);

        createProduct("SPEC-1", "Running Shoes", "Great for jogging", ProductStatus.ACTIVE, "10.000");
        createProduct("SPEC-2", "Winter Coat", "Warm and cozy", ProductStatus.DRAFT, "50.000");
        entityManager.flush();
    }

    private void createProduct(String sku, String name, String desc, ProductStatus status, String price) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setDescription(desc);
        p.setCategory(category);
        p.setPrice(new Money(new BigDecimal(price), "KWD"));
        p.setStatus(status);
        entityManager.persist(p);
    }

    @Test
    void hasText_shouldMatchNameCaseInsensitive() {
        ProductSearchCriteria criteria = new ProductSearchCriteria("RUNNING", null, null, null, null);
        List<Product> result = productRepository.findAll(ProductSpecification.matching(criteria));
        assertThat(result).extracting(Product::getSku).containsExactly("SPEC-1");
    }

    @Test
    void hasText_shouldMatchDescriptionCaseInsensitive() {
        ProductSearchCriteria criteria = new ProductSearchCriteria("cozy", null, null, null, null);
        List<Product> result = productRepository.findAll(ProductSpecification.matching(criteria));
        assertThat(result).extracting(Product::getSku).containsExactly("SPEC-2");
    }

    @Test
    void hasCategoryId_shouldFilterByCategory() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, category.getId(), null, null, null);
        List<Product> result = productRepository.findAll(ProductSpecification.matching(criteria));
        assertThat(result).hasSize(2);
    }

    @Test
    void hasStatus_shouldFilterByStatus() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, ProductStatus.ACTIVE, null, null);
        List<Product> result = productRepository.findAll(ProductSpecification.matching(criteria));
        assertThat(result).extracting(Product::getSku).containsExactly("SPEC-1");
    }
/*
    @Test
    void priceBetween_shouldHandleAllFourCases() {
        assertThat(productRepository.findAll(ProductSpecification.matching(
            new ProductSearchCriteria(null, null, null, null, null)))).hasSize(2);

        assertThat(productRepository.findAll(ProductSpecification.matching(
            new ProductSearchCriteria(null, null, null, new BigDecimal("20"), null))))
            .extracting(Product::getSku).containsExactly("SPEC-2");

        assertThat(productRepository.findAll(ProductSpecification.matching(
            new ProductSearchCriteria(null, null, null, null, new BigDecimal("20")))))
            .extracting(Product::getSku).containsExactly("SPEC-1");

        assertThat(productRepository.findAll(ProductSpecification.matching(
            new ProductSearchCriteria(null, null, null, new BigDecimal("5"), new BigDecimal("15")))))
            .extracting(Product::getSku).containsExactly("SPEC-1");
    }

 */
}
