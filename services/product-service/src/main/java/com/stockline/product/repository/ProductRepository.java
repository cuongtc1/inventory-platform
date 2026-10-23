package com.stockline.product.repository;

import com.stockline.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * {@link JpaSpecificationExecutor} adds {@code findAll(Specification, Pageable)}: a query whose WHERE clause is
 * built at runtime from the filters the client sent (see {@link ProductSpecifications}).
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	/**
	 * Same method as in {@link JpaSpecificationExecutor}, redeclared only to add the entity graph:
	 * category and supplier are loaded in the same query instead of two extra queries per product.
	 */
	@Override
	@EntityGraph(attributePaths = {"category", "supplier"})
	Page<Product> findAll(Specification<Product> spec, Pageable pageable);

	// Case-insensitive through MySQL's default collation, like the unique key uk_products_sku
	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, Long id);

	boolean existsByCategoryId(Long categoryId);

	boolean existsBySupplierId(Long supplierId);
}
