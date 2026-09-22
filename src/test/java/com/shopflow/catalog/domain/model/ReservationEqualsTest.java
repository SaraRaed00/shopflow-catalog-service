package com.shopflow.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationEqualsTest {

    @Test
    void equals_shouldReturnTrue_whenSameInstance() {
        Reservation reservation = new Reservation();
        assertThat(reservation.equals(reservation)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToNull() {
        Reservation reservation = new Reservation();
        assertThat(reservation.equals(null)).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenComparedToDifferentType() {
        Reservation reservation = new Reservation();
        assertThat(reservation.equals("not a reservation")).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenIdIsNull() {
        Reservation r1 = new Reservation();
        Reservation r2 = new Reservation();
        assertThat(r1.equals(r2)).isFalse();
    }

    @Test
    void equals_shouldReturnTrue_whenIdsMatch() {
        Reservation r1 = new Reservation();
        r1.setId(1L);
        Reservation r2 = new Reservation();
        r2.setId(1L);
        assertThat(r1.equals(r2)).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenIdsDiffer() {
        Reservation r1 = new Reservation();
        r1.setId(1L);
        Reservation r2 = new Reservation();
        r2.setId(2L);
        assertThat(r1.equals(r2)).isFalse();
    }

    @Test
    void hashCode_shouldBeConsistent_regardlessOfId() {
        Reservation r1 = new Reservation();
        Reservation r2 = new Reservation();
        r2.setId(99L);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }
}
