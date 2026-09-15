package com.shopflow.catalog.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stock_item")
@Getter
@Setter
public class StockItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false) private Product product;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id" , nullable = false) private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    @Version
    @Column(nullable = false)
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockItem other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }



}
