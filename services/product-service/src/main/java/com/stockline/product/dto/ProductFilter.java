package com.stockline.product.dto;

import com.stockline.product.entity.ProductStatus;

/**
 * Optional filters of GET /api/products, filled from query parameters with the same names:
 * {@code ?search=cable&categoryId=3&supplierId=5&status=ACTIVE}. A null field means "don't filter on this".
 */
public record ProductFilter(
		String search,
		Long categoryId,
		Long supplierId,
		ProductStatus status
) {
}
