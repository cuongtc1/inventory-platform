package com.stockline.product.repository;

import com.stockline.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

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

	// Derived count query: SELECT COUNT(*) FROM products WHERE category_id = ?
	long countByCategoryId(Long categoryId);

	/**
	 * Product counts for a page of categories in one query, instead of one count query per category.
	 * Categories without products are not in the result (count 0).
	 *
	 * <p>The result rows are an interface projection: Spring Data creates objects of {@link CategoryProductCount}
	 * and maps the {@code AS} aliases to its getters, no entity or DTO class needed.
	 */
	@Query("""
			SELECT p.category.id AS categoryId, COUNT(p) AS productCount
			FROM Product p
			WHERE p.category.id IN :categoryIds
			GROUP BY p.category.id
			""")
	List<CategoryProductCount> countByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);

	interface CategoryProductCount {

		Long getCategoryId();

		long getProductCount();
	}
}
