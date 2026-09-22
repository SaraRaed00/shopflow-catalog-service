package com.shopflow.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEqualsTest {

    @Test
    void equals_shouldReturnTrue_whenSameInstance() {
        Product product = new Product();
        assertThat(product.equals(product)).isTrue(); // hits "this == o"
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToNull() {
        Product product = new Product();
        assertThat(product.equals(null)).isFalse(); // hits "!(o instanceof Product)"
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToDifferentType() {
        Product product = new Product();
        assertThat(product.equals("not a product")).isFalse(); // same branch, different way in
    }

    @Test
    void equals_shouldReturnFalse_whenIdIsNull() {
        Product p1 = new Product();
        Product p2 = new Product();
        assertThat(p1.equals(p2)).isFalse(); // hits "id != null" being false
    }

    @Test
    void equals_shouldReturnTrue_whenIdsMatch() {
        Product p1 = new Product();
        p1.setId(1L);
        Product p2 = new Product();
        p2.setId(1L);
        assertThat(p1.equals(p2)).isTrue(); // hits "id.equals(other.id)" true path
    }

    @Test
    void equals_shouldReturnFalse_whenIdsDiffer() {
        Product p1 = new Product();
        p1.setId(1L);
        Product p2 = new Product();
        p2.setId(2L);
        assertThat(p1.equals(p2)).isFalse(); // same line, false path
    }

    @Test
    void hashCode_shouldBeConsistent_regardlessOfId() {
        Product p1 = new Product();
        Product p2 = new Product();
        p2.setId(99L);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode()); // always getClass().hashCode()
    }
}
