package com.shopflow.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemEqualsTest {

    @Test
    void equals_shouldReturnTrue_whenSameInstance() {
        StockItem item = new StockItem();
        assertThat(item.equals(item)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToNull() {
        StockItem item = new StockItem();
        assertThat(item.equals(null)).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToDifferentType() {
        StockItem item = new StockItem();
        assertThat(item.equals("not a stock item")).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenIdIsNull() {
        StockItem i1 = new StockItem();
        StockItem i2 = new StockItem();
        assertThat(i1.equals(i2)).isFalse();
    }

    @Test
    void equals_shouldReturnTrue_whenIdsMatch() {
        StockItem i1 = new StockItem();
        i1.setId(1L);
        StockItem i2 = new StockItem();
        i2.setId(1L);
        assertThat(i1.equals(i2)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenIdsDiffer() {
        StockItem i1 = new StockItem();
        i1.setId(1L);
        StockItem i2 = new StockItem();
        i2.setId(2L);
        assertThat(i1.equals(i2)).isFalse();
    }

    @Test
    void hashCode_shouldBeConsistent_regardlessOfId() {
        StockItem i1 = new StockItem();
        StockItem i2 = new StockItem();
        i2.setId(99L);
        assertThat(i1.hashCode()).isEqualTo(i2.hashCode());
    }
}
