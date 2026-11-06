package com.stockline.inventory.repository;

import com.stockline.inventory.dto.StockLevelFilter;
import com.stockline.inventory.entity.StockLevel;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the WHERE clause of the stock list from the filters that were sent, like ProductSpecifications.
 */
public final class StockLevelSpecifications {

	static final char LIKE_ESCAPE = '\\';

	private StockLevelSpecifications() {
	}

	public static Specification<StockLevel> matching(StockLevelFilter filter) {
		List<Specification<StockLevel>> conditions = new ArrayList<>();
		if (filter.search() != null && !filter.search().isBlank()) {
			conditions.add(nameOrSkuContains(filter.search().strip()));
		}
		if (filter.warehouseId() != null) {
			conditions.add(inWarehouse(filter.warehouseId()));
		}
		if (filter.productId() != null) {
			conditions.add(forProduct(filter.productId()));
		}
		if (Boolean.TRUE.equals(filter.lowStock())) {
			conditions.add(lowStock());
		}
		return Specification.allOf(conditions);
	}

	static Specification<StockLevel> nameOrSkuContains(String text) {
		String pattern = containsPattern(text);
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("productName")), pattern, LIKE_ESCAPE),
				cb.like(cb.lower(root.get("productSku")), pattern, LIKE_ESCAPE));
	}

	static Specification<StockLevel> inWarehouse(Long warehouseId) {
		return (root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId);
	}

	static Specification<StockLevel> forProduct(Long productId) {
		return (root, query, cb) -> cb.equal(root.get("productId"), productId);
	}

	/** {@code on_hand - reserved <= reorder_level}: the same rule as {@code StockLevel.isLowStock()}, in SQL. */
	static Specification<StockLevel> lowStock() {
		return (root, query, cb) -> cb.le(
				cb.diff(root.<Integer>get("onHand"), root.<Integer>get("reserved")),
				root.<Integer>get("reorderLevel"));
	}

	/** A lower-case LIKE pattern for "contains". {@code %} and {@code _} are wildcards; escaped, they are plain text. */
	static String containsPattern(String text) {
		String lowerCase = text.toLowerCase(Locale.ROOT);
		return "%" + lowerCase.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}
}
