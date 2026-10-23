package com.stockline.product.repository;

import com.stockline.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data creates the implementation at startup: save, findById, findAll (with paging), delete, ...
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
