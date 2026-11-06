package com.stockline.inventory.repository;

import com.stockline.common.util.DateTimeUtils;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.dto.StockMovementFilter;
import com.stockline.inventory.entity.StockMovement;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause of the movements list from the filters that were sent, like StockLevelSpecifications.
 */
public final class StockMovementSpecifications {

	private StockMovementSpecifications() {
	}

	public static Specification<StockMovement> matching(StockMovementFilter filter) {
		List<Specification<StockMovement>> conditions = new ArrayList<>();
		if (filter.search() != null && !filter.search().isBlank()) {
			conditions.add(nameOrSkuContains(filter.search().strip()));
		}
		if (filter.warehouseId() != null) {
			conditions.add(inWarehouse(filter.warehouseId()));
		}
		if (filter.productId() != null) {
			conditions.add(forProduct(filter.productId()));
		}
		if (filter.movementType() != null) {
			conditions.add(ofType(filter.movementType()));
		}
		if (filter.from() != null) {
			conditions.add(onOrAfter(filter.from()));
		}
		if (filter.to() != null) {
			conditions.add(onOrBefore(filter.to()));
		}
		return Specification.allOf(conditions);
	}

	static Specification<StockMovement> nameOrSkuContains(String text) {
		String pattern = StockLevelSpecifications.containsPattern(text);
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("productName")), pattern, StockLevelSpecifications.LIKE_ESCAPE),
				cb.like(cb.lower(root.get("productSku")), pattern, StockLevelSpecifications.LIKE_ESCAPE));
	}

	static Specification<StockMovement> inWarehouse(Long warehouseId) {
		return (root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId);
	}

	static Specification<StockMovement> forProduct(Long productId) {
		return (root, query, cb) -> cb.equal(root.get("productId"), productId);
	}

	static Specification<StockMovement> ofType(MovementType movementType) {
		return (root, query, cb) -> cb.equal(root.get("movementType"), movementType);
	}

	/** From midnight of that Vietnam day, which is still the day before in UTC. */
	static Specification<StockMovement> onOrAfter(LocalDate date) {
		Instant start = DateTimeUtils.startOfDay(date);
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start);
	}

	/** Before midnight of the next Vietnam day, so the whole of {@code date} is included. */
	static Specification<StockMovement> onOrBefore(LocalDate date) {
		Instant end = DateTimeUtils.endOfDayExclusive(date);
		return (root, query, cb) -> cb.lessThan(root.get("createdAt"), end);
	}
}
