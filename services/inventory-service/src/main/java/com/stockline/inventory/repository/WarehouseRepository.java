package com.stockline.inventory.repository;

import com.stockline.inventory.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

	/** Part of the code or the name, ignoring case; Spring Data escapes % and _ in the text. */
	Page<Warehouse> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name, Pageable pageable);

	// Case-insensitive through MySQL's default collation, like the unique key uk_warehouses_code
	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	/**
	 * The number of products in stock for a page of warehouses, in one query instead of one per warehouse.
	 * Warehouses without stock levels are not in the result (count 0). Plain SQL for now: the {@code StockLevel}
	 * entity comes with T4.3.
	 */
	@Query(value = """
			SELECT warehouse_id AS warehouseId, COUNT(*) AS skuCount
			FROM stock_levels
			WHERE warehouse_id IN (:warehouseIds)
			GROUP BY warehouse_id
			""", nativeQuery = true)
	List<WarehouseSkuCount> countSkus(@Param("warehouseIds") Collection<Long> warehouseIds);

	interface WarehouseSkuCount {

		long getWarehouseId();

		long getSkuCount();
	}
}
