package com.stockline.inventory.repository;

import com.stockline.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>,
		JpaSpecificationExecutor<StockMovement> {

	/** Redeclared only to add the entity graph: the warehouse comes in the same query, not one extra per row. */
	@Override
	@EntityGraph(attributePaths = "warehouse")
	Page<StockMovement> findAll(Specification<StockMovement> spec, Pageable pageable);
}
