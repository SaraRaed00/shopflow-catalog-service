package com.shopflow.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseEqualsTest {

    @Test
    void equals_shouldReturnTrue_whenSameInstance() {
        Warehouse warehouse = new Warehouse();
        assertThat(warehouse.equals(warehouse)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToNull() {
        Warehouse warehouse = new Warehouse();
        assertThat(warehouse.equals(null)).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToDifferentType() {
        Warehouse warehouse = new Warehouse();
        assertThat(warehouse.equals("not a warehouse")).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenIdIsNull() {
        Warehouse w1 = new Warehouse();
        Warehouse w2 = new Warehouse();
        assertThat(w1.equals(w2)).isFalse();
    }

    @Test
    void equals_shouldReturnTrue_whenIdsMatch() {
        Warehouse w1 = new Warehouse();
        w1.setId(1L);
        Warehouse w2 = new Warehouse();
        w2.setId(1L);
        assertThat(w1.equals(w2)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenIdsDiffer() {
        Warehouse w1 = new Warehouse();
        w1.setId(1L);
        Warehouse w2 = new Warehouse();
        w2.setId(2L);
        assertThat(w1.equals(w2)).isFalse();
    }

    @Test
    void hashCode_shouldBeConsistent_regardlessOfId() {
        Warehouse w1 = new Warehouse();
        Warehouse w2 = new Warehouse();
        w2.setId(99L);
        assertThat(w1.hashCode()).isEqualTo(w2.hashCode());
    }
}
