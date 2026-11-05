package com.stockline.inventory.dto;

import java.time.Instant;

/**
 * A warehouse as returned by the API. {@code address} is null when not set.
 * {@code skuCount} is the number of different products with a stock level in this warehouse.
 * {@code createdBy} / {@code updatedBy} are the user's login, or "system".
 */
public record WarehouseResponse(
		Long id,
		String code,
		String name,
		String address,
		long skuCount,
		Instant createdAt,
		String createdBy,
		Instant updatedAt,
		String updatedBy
) {
}
