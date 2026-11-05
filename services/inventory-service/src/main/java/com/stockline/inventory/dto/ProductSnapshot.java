package com.stockline.inventory.dto;

/**
 * The fields of product-service's product that this service copies. Other JSON fields are ignored.
 */
public record ProductSnapshot(
		Long id,
		String sku,
		String name,
		int reorderLevel
) {
}
