package com.stockline.inventory.dto;

/**
 * Optional filters of GET /api/stock-levels, filled from query parameters with the same names:
 * {@code ?search=mouse&warehouseId=1&productId=12&lowStock=true}. A null field means "don't filter on this".
 */
public record StockLevelFilter(
		String search,
		Long warehouseId,
		Long productId,
		Boolean lowStock
) {
}
