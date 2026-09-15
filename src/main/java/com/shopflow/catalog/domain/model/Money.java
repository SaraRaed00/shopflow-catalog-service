package com.shopflow.catalog.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Money {
// Embeddable will be inside the product table, gets inlined as columns to the table using it.
    @Column(name = "price_amount", nullable = false, precision = 12, scale = 3)
    private BigDecimal amount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String currency;
}
