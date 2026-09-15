package com.shopflow.catalog.repository;

import com.shopflow.catalog.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {


}
