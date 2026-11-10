package com.stockline.order.dto;

import java.math.BigDecimal;

/**
 * The fields of product-service's product that an order copies. Other JSON fields are ignored. {@code status} stays
 * a string: product-service owns that enum, and this service only needs to know "ACTIVE or not".
 */
public record ProductSnapshot(
		Long id,
		String sku,
		String name,
		BigDecimal salePrice,
		String status
) {

	public boolean isActive() {
		return "ACTIVE".equals(status);
	}
}
