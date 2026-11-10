package com.stockline.order.dto;

/**
 * The fields of inventory-service's warehouse that an order needs. Other JSON fields are ignored.
 */
public record WarehouseSnapshot(
		Long id,
		String code
) {
}
