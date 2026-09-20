package com.shopflow.catalog.repository;

import com.shopflow.catalog.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> , JpaSpecificationExecutor<Product> {
    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);
    boolean existsByCategoryId(Long CategoryId);
}
