package com.stockline.inventory.repository;

import com.stockline.inventory.entity.StockLevel;
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
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long>, JpaSpecificationExecutor<StockLevel> {

	/** Redeclared only to add the entity graph: the warehouse comes in the same query, not one extra per row. */
	@Override
	@EntityGraph(attributePaths = "warehouse")
	Page<StockLevel> findAll(Specification<StockLevel> spec, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "warehouse")
	Optional<StockLevel> findById(Long id);

	Optional<StockLevel> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

	/**
	 * The number of products in stock for a page of warehouses, in one query instead of one per warehouse.
	 * Warehouses without stock levels are not in the result (count 0).
	 */
	@Query("""
			SELECT s.warehouse.id AS warehouseId, COUNT(s) AS skuCount
			FROM StockLevel s
			WHERE s.warehouse.id IN :warehouseIds
			GROUP BY s.warehouse.id
			""")
	List<WarehouseSkuCount> countByWarehouseIds(@Param("warehouseIds") Collection<Long> warehouseIds);

	interface WarehouseSkuCount {

		long getWarehouseId();

		long getSkuCount();
	}
}
