package com.shopflow.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryEqualsTest {

    @Test
    void equals_shouldReturnTrue_whenSameInstance() {
        Category category = new Category();
        assertThat(category.equals(category)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToNull() {
        Category category = new Category();
        assertThat(category.equals(null)).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToDifferentType() {
        Category category = new Category();
        assertThat(category.equals("not a category")).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenIdIsNull() {
        Category c1 = new Category();
        Category c2 = new Category();
        assertThat(c1.equals(c2)).isFalse();
    }

    @Test
    void equals_shouldReturnTrue_whenIdsMatch() {
        Category c1 = new Category();
        c1.setId(1L);
        Category c2 = new Category();
        c2.setId(1L);
        assertThat(c1.equals(c2)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenIdsDiffer() {
        Category c1 = new Category();
        c1.setId(1L);
        Category c2 = new Category();
        c2.setId(2L);
        assertThat(c1.equals(c2)).isFalse();
    }

    @Test
    void hashCode_shouldBeConsistent_regardlessOfId() {
        Category c1 = new Category();
        Category c2 = new Category();
        c2.setId(99L);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }
}
