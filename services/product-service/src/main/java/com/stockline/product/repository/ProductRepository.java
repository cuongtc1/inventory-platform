package com.stockline.product.repository;

import com.stockline.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	boolean existsByCategoryId(Long categoryId);

	boolean existsBySupplierId(Long supplierId);
}
