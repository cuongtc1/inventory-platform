package com.stockline.inventory.repository;

import com.stockline.inventory.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

	/** Part of the code or the name, ignoring case; Spring Data escapes % and _ in the text. */
	Page<Warehouse> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name, Pageable pageable);

	// Case-insensitive through MySQL's default collation, like the unique key uk_warehouses_code
	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);
}
