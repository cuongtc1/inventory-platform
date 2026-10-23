package com.stockline.product.repository;

import com.stockline.product.constant.ProductStatus;
import com.stockline.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause for the product list. Each filter is a small {@link Specification} (one condition);
 * only the filters the client sent are combined with AND. No filters means no WHERE clause.
 */
public final class ProductSpecifications {

	private static final char LIKE_ESCAPE = '\\';

	private ProductSpecifications() {
	}

	public static Specification<Product> matching(String search, Long categoryId, Long supplierId,
			ProductStatus status) {
		List<Specification<Product>> conditions = new ArrayList<>();
		if (search != null && !search.isBlank()) {
			conditions.add(nameOrSkuContains(search.strip()));
		}
		if (categoryId != null) {
			conditions.add(hasCategory(categoryId));
		}
		if (supplierId != null) {
			conditions.add(hasSupplier(supplierId));
		}
		if (status != null) {
			conditions.add(hasStatus(status));
		}
		return Specification.allOf(conditions);
	}

	/**
	 * {@code LOWER(name) LIKE '%text%' OR LOWER(sku) LIKE '%text%'}.
	 */
	static Specification<Product> nameOrSkuContains(String text) {
		String pattern = "%" + escapeLike(text.toLowerCase()) + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
				cb.like(cb.lower(root.get("sku")), pattern, LIKE_ESCAPE));
	}

	static Specification<Product> hasCategory(Long categoryId) {
		return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
	}

	static Specification<Product> hasSupplier(Long supplierId) {
		return (root, query, cb) -> cb.equal(root.get("supplier").get("id"), supplierId);
	}

	static Specification<Product> hasStatus(ProductStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	/**
	 * In LIKE, {@code %} and {@code _} are wildcards. Escape them so searching "50%" finds the text "50%"
	 * instead of everything starting with "50".
	 */
	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
