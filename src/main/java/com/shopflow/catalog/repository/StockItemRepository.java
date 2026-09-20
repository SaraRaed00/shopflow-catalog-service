package com.shopflow.catalog.repository;

import com.shopflow.catalog.domain.model.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Optional<StockItem> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    List<StockItem> findByProductId(Long productId);
}
