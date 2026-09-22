package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    @Test
    void toResponse_shouldMapAllFieldsCorrectly() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Electronics");

        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU-1");
        product.setName("Widget");
        product.setDescription("desc");
        product.setCategory(category);
        product.setPrice(new Money(new BigDecimal("9.999"), "KWD"));
        product.setStatus(ProductStatus.ACTIVE);

        var response = mapper.toResponse(product);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.categoryId()).isEqualTo(5L);
        assertThat(response.categoryName()).isEqualTo("Electronics");
        assertThat(response.priceAmount()).isEqualTo(new BigDecimal("9.999"));
        assertThat(response.priceCurrency()).isEqualTo("KWD");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }
}
