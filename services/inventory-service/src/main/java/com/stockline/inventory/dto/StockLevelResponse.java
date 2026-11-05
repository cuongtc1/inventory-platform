package com.stockline.inventory.dto;

import java.time.Instant;

/**
 * One product in one warehouse. {@code available} is {@code onHand - reserved}; {@code lowStock} is true when
 * available is at or below {@code reorderLevel}. {@code productSku}, {@code productName} and {@code reorderLevel} are
 * copies from product-service, refreshed at each adjustment.
 */
public record StockLevelResponse(
		Long id,
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		Long productId,
		String productSku,
		String productName,
		int onHand,
		int reserved,
		int available,
		int reorderLevel,
		boolean lowStock,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {
}
