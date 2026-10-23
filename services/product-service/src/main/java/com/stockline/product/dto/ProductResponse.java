package com.stockline.product.dto;

import com.stockline.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A product as returned by the API, with the category and supplier names so a list needs no extra calls.
 * {@code supplierId} and {@code supplierName} are null when no supplier is set.
 */
public record ProductResponse(
		Long id,
		String sku,
		String name,
		Long categoryId,
		String categoryName,
		Long supplierId,
		String supplierName,
		BigDecimal costPrice,
		BigDecimal salePrice,
		int reorderLevel,
		ProductStatus status,
		Instant createdAt,
		Instant updatedAt
) {
}
